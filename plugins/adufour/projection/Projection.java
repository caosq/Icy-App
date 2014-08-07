package plugins.adufour.projection;

import icy.image.IcyBufferedImage;
import icy.math.ArrayMath;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.util.OMEUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.VarSequence;

public class Projection extends EzPlug implements Block
{
    public enum ProjectionDirection
    {
        Z, T
    }
    
    public enum ProjectionType
    {
        MAX("Maximum"), MEAN("Average"), MED("Median"), MIN("Minimum"), STD("Standard Deviation"), SATSUM("Saturated Sum");
        
        private final String description;
        
        ProjectionType(String description)
        {
            this.description = description;
        }
        
        public String toString()
        {
            return description;
        }
    }
    
    private final EzVarSequence                  input          = new EzVarSequence("Input");
    
    private final EzVarEnum<ProjectionDirection> projectionDir  = new EzVarEnum<Projection.ProjectionDirection>("Project along", ProjectionDirection.values(), ProjectionDirection.Z);
    
    private final EzVarEnum<ProjectionType>      projectionType = new EzVarEnum<Projection.ProjectionType>("Projection type", ProjectionType.values(), ProjectionType.MAX);
    
    private final EzVarBoolean                   restrictToROI  = new EzVarBoolean("Restrict to ROI", false);
    
    private final VarSequence                    output         = new VarSequence("projected sequence", null);
    
    @Override
    protected void initialize()
    {
        addEzComponent(input);
        addEzComponent(projectionDir);
        addEzComponent(projectionType);
        
        restrictToROI.setToolTipText("Check this option to project only the intensity data contained within the sequence ROI");
        addEzComponent(restrictToROI);
    }
    
    @Override
    protected void execute()
    {
        switch (projectionDir.getValue())
        {
            case T:
                output.setValue(tProjection(input.getValue(true), projectionType.getValue(), true, restrictToROI.getValue()));
            break;
            case Z:
                output.setValue(zProjection(input.getValue(true), projectionType.getValue(), true, restrictToROI.getValue()));
            break;
            default:
                throw new UnsupportedOperationException("Projection along " + projectionDir.getValue() + " not supported");
        }
        
        if (getUI() != null) addSequence(output.getValue());
    }
    
    @Override
    public void clean()
    {
        
    }
    
    /**
     * Performs a Z projection of the input sequence using the specified algorithm. If the sequence
     * is already 2D, then a copy of the sequence is returned
     * 
     * @param sequence
     *            the sequence to project
     * @param projection
     *            the type of projection to perform (see {@link ProjectionType} enumeration)
     * @param multiThread
     *            true if the process should be multi-threaded
     * @return the projected sequence
     */
    public Sequence zProjection(final Sequence in, final ProjectionType projection, boolean multiThread)
    {
        return zProjection(in, projection, multiThread, false);
    }
    
    /**
     * Performs a Z projection of the input sequence using the specified algorithm. If the sequence
     * is already 2D, then a copy of the sequence is returned
     * 
     * @param sequence
     *            the sequence to project
     * @param projection
     *            the type of projection to perform (see {@link ProjectionType} enumeration)
     * @param multiThread
     *            true if the process should be multi-threaded
     * @param restrictToROI
     *            <code>true</code> projects only data located within the sequence ROI,
     *            <code>false</code> projects the entire data set
     * @return the projected sequence
     */
    public Sequence zProjection(final Sequence in, final ProjectionType projection, boolean multiThread, final boolean restrictToROI)
    {
        final int depth = in.getSizeZ();
        if (depth == 1 && !restrictToROI) return SequenceUtil.getCopy(in);
        
        final Sequence out = new Sequence(OMEUtil.createOMEMetadata(in.getMetadata()));
        
        out.setName(projection.name() + " projection of " + in.getName());
        
        final int width = in.getSizeX();
        final int height = in.getSizeY();
        final int length = in.getSizeT();
        final int channels = in.getSizeC();
        final DataType dataType = in.getDataType_();
        
        final List<ROI> rois = in.getROIs();
        
        int cpu = Runtime.getRuntime().availableProcessors();
        
        // this plug-in processes each channel of each stack in a separate thread
        
        ExecutorService service = multiThread ? Executors.newFixedThreadPool(cpu) : Executors.newSingleThreadExecutor();
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(channels * length);
        
        for (int t = 0; t < length; t++)
        {
            final int time = t;
            
            out.setImage(time, 0, new IcyBufferedImage(width, height, channels, dataType));
            
            for (int c = 0; c < channels; c++)
            {
                out.getColorModel().setColorMap(c, in.getColorModel().getColorMap(c), true);
                
                final int channel = c;
                
                futures.add(service.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Object in_Z_XY = in.getDataXYZ(time, channel);
                        Object out_Z_XY = out.getDataXY(time, 0, channel);
                        
                        double[] projectedBuffer = new double[width * height];
                        double[] lineToProject = new double[depth];
                        
                        int off = 0;
                        for (int y = 0; y < height; y++)
                        {
                            for (int x = 0; x < width; x++, off++)
                            {
                                if (restrictToROI && rois.size() > 0)
                                {
                                    int nbValues = 0;
                                    
                                    for (int z = 0; z < depth; z++)
                                    {
                                        for (ROI roi : rois)
                                        {
                                            if (roi.contains(x, y, z, time, channel))
                                            {
                                                lineToProject[nbValues++] = Array1DUtil.getValue(((Object[]) in_Z_XY)[z], off, dataType);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (nbValues == 0) continue;
                                    
                                    if (nbValues < lineToProject.length) lineToProject = Arrays.copyOf(lineToProject, nbValues);
                                }
                                else
                                {
                                    for (int z = 0; z < depth; z++)
                                        lineToProject[z] = Array1DUtil.getValue(((Object[]) in_Z_XY)[z], off, dataType);
                                }
                                
                                switch (projection)
                                {
                                    case MAX:
                                        projectedBuffer[off] = ArrayMath.max(lineToProject);
                                    break;
                                    case MEAN:
                                        projectedBuffer[off] = ArrayMath.mean(lineToProject);
                                    break;
                                    case MED:
                                        projectedBuffer[off] = ArrayMath.median(lineToProject, false);
                                    break;
                                    case MIN:
                                        projectedBuffer[off] = ArrayMath.min(lineToProject);
                                    break;
                                    case STD:
                                        projectedBuffer[off] = ArrayMath.std(lineToProject, true);
                                    break;
                                    case SATSUM:
                                        projectedBuffer[off] = ArrayMath.sum(lineToProject);
                                    break;
                                    default:
                                        throw new UnsupportedOperationException(projection + " intensity projection not implemented");
                                }
                            } // x
                        } // y
                        
                        Array1DUtil.doubleArrayToSafeArray(projectedBuffer, out_Z_XY, dataType.isSigned());
                    }
                }));
            }
        }
        
        try
        {
            for (Future<?> future : futures)
                future.get();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
        
        service.shutdown();
        
        out.updateChannelsBounds(true);
        return out;
    }
    
    /**
     * Performs a T projection of the input sequence using the specified algorithm. If the sequence
     * has only one time point, then a copy of the sequence is returned
     * 
     * @param sequence
     *            the sequence to project
     * @param projection
     *            the type of projection to perform (see {@link ProjectionType} enumeration)
     * @param multiThread
     *            true if the process should be multi-threaded
     * @return the projected sequence
     */
    public Sequence tProjection(final Sequence in, final ProjectionType projection, boolean multiThread)
    {
        return tProjection(in, projection, multiThread, false);
    }
    
    /**
     * Performs a T projection of the input sequence using the specified algorithm. If the sequence
     * has only one time point, then a copy of the sequence is returned
     * 
     * @param sequence
     *            the sequence to project
     * @param projection
     *            the type of projection to perform (see {@link ProjectionType} enumeration)
     * @param multiThread
     *            true if the process should be multi-threaded
     * @param restrictToROI
     *            <code>true</code> projects only data located within the sequence ROI,
     *            <code>false</code> projects the entire data set
     * @return the projected sequence
     */
    public Sequence tProjection(final Sequence in, final ProjectionType projection, boolean multiThread, final boolean restrictToROI)
    {
        final int length = in.getSizeT();
        if (length == 1 && !restrictToROI) return SequenceUtil.getCopy(in);
        
        final Sequence out = new Sequence();
        out.setName(projection.name() + " projection of " + in.getName());
        
        final int width = in.getSizeX();
        final int height = in.getSizeY();
        final int depth = in.getSizeZ();
        final int channels = in.getSizeC();
        final DataType dataType = in.getDataType_();
        
        final List<ROI> rois = in.getROIs();
        
        int cpu = Runtime.getRuntime().availableProcessors();
        
        // this plug-in processes each channel of each slice in a separate thread
        
        ExecutorService service = multiThread ? Executors.newFixedThreadPool(cpu * 2) : Executors.newSingleThreadExecutor();
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(channels * depth);
        
        for (int z = 0; z < depth; z++)
        {
            final int slice = z;
            
            out.setImage(0, slice, new IcyBufferedImage(width, height, channels, dataType));
            
            for (int c = 0; c < channels; c++)
            {
                if (slice == 0) out.getColorModel().setColorMap(c, in.getColorModel().getColorMap(c), true);
                
                final int channel = c;
                
                futures.add(service.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Object in_T_Z_XY = in.getDataXYZT(channel);
                        Object out_Z_XY = out.getDataXY(0, slice, channel);
                        
                        double[] projectedBuffer = new double[width * height];
                        double[] lineToProject = new double[length];
                        
                        int off = 0;
                        for (int y = 0; y < height; y++)
                        {
                            for (int x = 0; x < width; x++, off++)
                            {
                                if (restrictToROI && rois.size() > 0)
                                {
                                    int nbValues = 0;
                                    
                                    for (int t = 0; t < length; t++)
                                    {
                                        for (ROI roi : rois)
                                        {
                                            if (roi.contains(x, y, slice, t, channel))
                                            {
                                                lineToProject[nbValues++] = Array1DUtil.getValue(((Object[][]) in_T_Z_XY)[t][slice], off, dataType);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (nbValues == 0) continue;
                                    
                                    if (nbValues < lineToProject.length) lineToProject = Arrays.copyOf(lineToProject, nbValues);
                                }
                                else
                                {
                                    for (int t = 0; t < length; t++)
                                        lineToProject[t] = Array1DUtil.getValue(((Object[][]) in_T_Z_XY)[t][slice], off, dataType);
                                }
                                
                                switch (projection)
                                {
                                    case MAX:
                                        projectedBuffer[off] = ArrayMath.max(lineToProject);
                                    break;
                                    case MEAN:
                                        projectedBuffer[off] = ArrayMath.mean(lineToProject);
                                    break;
                                    case MED:
                                        projectedBuffer[off] = ArrayMath.median(lineToProject, false);
                                    break;
                                    case MIN:
                                        projectedBuffer[off] = ArrayMath.min(lineToProject);
                                    break;
                                    case STD:
                                        projectedBuffer[off] = ArrayMath.std(lineToProject, true);
                                    break;
                                    case SATSUM:
                                        projectedBuffer[off] = ArrayMath.sum(lineToProject);
                                    break;
                                    default:
                                        throw new UnsupportedOperationException(projection + " intensity projection not implemented");
                                }
                            } // x
                        } // y
                        
                        Array1DUtil.doubleArrayToSafeArray(projectedBuffer, out_Z_XY, dataType.isSigned());
                    }
                }));
            }
        }
        
        try
        {
            for (Future<?> future : futures)
                future.get();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
        
        service.shutdown();
        
        out.updateChannelsBounds(true);
        return out;
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input", input.getVariable());
        inputMap.add("projection direction", projectionDir.getVariable());
        inputMap.add("projection type", projectionType.getVariable());
        inputMap.add("restrict to ROI", restrictToROI.getVariable());
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("projection output", output);
    }
    
}
