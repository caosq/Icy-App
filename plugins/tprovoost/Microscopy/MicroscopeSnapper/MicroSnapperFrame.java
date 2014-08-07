package plugins.tprovoost.Microscopy.MicroscopeSnapper;

import icy.gui.component.button.IcyButton;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.main.ActiveSequenceListener;
import icy.gui.main.GlobalSequenceListener;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceUtil;
import icy.system.IcyExceptionHandler;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.Calendar;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.tprovoost.Microscopy.MicroManager.tools.FrameUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.ImageGetter;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover.StageListener;

/**
 * @author Irsath Nguyen
 */
public class MicroSnapperFrame extends IcyFrame implements StageListener, GlobalSequenceListener,
        ActiveSequenceListener
{
    private final static String TypeSnapZ = "snapZ", TypeSnapC = "snapC", TypeSnapT = "snapT";

    MicroscopeSnapperPlugin plugin;
    Sequence activeSequence = null;
    ActiveSequenceListener sequenceListener;
    double initialPos = 0;
    int _slices = 10;
    double _interval_ = 1.0D;
    String typeAction = TypeSnapT;
    /** Is the snapper already capturing images ? */
    boolean lockActiveSequence = false;

    // --------------
    // GUI Components
    // ---------------
    JLabel currentStagePosition, currentWorkingSequence;
    IcyButton _btn_lock_sequence;
    IcyButton _btn_snap;
    JRadioButton _rb_snap_z;
    JRadioButton _rb_snap_t;
    JRadioButton _rb_snap_c;
    JPanel panel_3d_options;

    public MicroSnapperFrame(MicroscopeSnapperPlugin plugin)
    {
        super("MicromanagerPlugin Snapper", true, true, false, true);

        this.plugin = plugin;

        activeSequence = null;
        _slices = 10;
        _interval_ = 1.0D;
        typeAction = TypeSnapT;
        lockActiveSequence = false;

        try
        {
            initialPos = StageMover.getZ();
        }
        catch (Exception e)
        {
            initialPos = 0;
        }

        initGUI();

        // add a listener to the frame in order to remove the plugin from
        // the GUI when the frame is closed
        addFrameListener(new IcyFrameAdapter()
        {
            @Override
            public void icyFrameClosed(IcyFrameEvent e)
            {
                MicroSnapperFrame.this.plugin.shutdown();
            }
        });

        StageMover.addListener(this);
        Icy.getMainInterface().addGlobalSequenceListener(this);
        Icy.getMainInterface().addActiveSequenceListener(this);
    }

    private void initGUI()
    {
        // ---------------
        // INFORMATION
        // ---------------
        final JPanel panel_info = GuiUtil.generatePanel("Position");
        panel_info.setLayout(new GridLayout(3, 2));
        currentStagePosition = new JLabel(StringUtil.toString(initialPos, 3) + " µm");

        final JTextField positionText = new JTextField(StringUtil.toString(initialPos));
        positionText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() != KeyEvent.VK_ENTER)
                    return;

                final double d = StringUtil.parseDouble(positionText.getText(), Double.NaN);

                if (Double.isNaN(d))
                    new FailedAnnounceFrame("Please enter a number for starting Z position !", 5);
                else
                {
                    try
                    {
                        StageMover.moveZAbsolute(d);
                        initialPos = d;
                    }
                    catch (Exception e1)
                    {
                        new FailedAnnounceFrame("Failed to move the device", 5);
                    }
                }
            }
        });
        panel_info.add(new JLabel("Start Z position (µm) :"));
        panel_info.add(positionText);
        panel_info.add(new JLabel("Current Z position (µm) : "));
        panel_info.add(currentStagePosition);
        panel_info.add(new JLabel("Current working sequence :"));
        currentWorkingSequence = new JLabel("null");
        panel_info.add(currentWorkingSequence);

        // -----------------
        // BUTTON CREATION
        // -----------------
        IcyButton _btn_createSnap = FrameUtils.createUIButton("Create a new sequence from snapped image.",
                "duplicate.png", new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ThreadUtil.bgRun(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                createNewSequence();
                            }
                        });
                        ;
                    }
                });

        _btn_snap = FrameUtils.createUIButton("Capture and add to the current sequence as new position.",
                "layers_v1.png", new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ThreadUtil.bgRun(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                addToActiveSequence();
                            }
                        });
                    }
                });
        _btn_snap.setEnabled(false);

        _btn_lock_sequence = FrameUtils.createUIButton("Lock the current working sequence.", "unlocked.png",
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (activeSequence == null)
                            return;
                        lockActiveSequence = !lockActiveSequence;
                        if (lockActiveSequence)
                        {
                            _btn_lock_sequence.setIcon(new IcyIcon(ResourceUtil.ICON_LOCK_CLOSE));
                            _btn_lock_sequence.setText("Unlock the current working sequence.");
                        }
                        else
                        {
                            _btn_lock_sequence.setIcon(new IcyIcon(ResourceUtil.ICON_LOCK_OPEN));
                            _btn_lock_sequence.setText("Lock the current working sequence.");
                        }
                    }
                });
        _btn_lock_sequence.setEnabled(false);

        JPanel panel_buttons = new JPanel();
        panel_buttons.setLayout(new GridLayout(3, 1));
        panel_buttons.add(_btn_createSnap);
        panel_buttons.add(_btn_snap);
        panel_buttons.add(_btn_lock_sequence);

        // ----------------
        // SNAP OPTIONS
        // ---------------
        ActionListener radioListener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (e.getActionCommand().equals(TypeSnapZ))
                    setPanelEnabled(panel_3d_options, false);
                else if (!panel_3d_options.isEnabled())
                    setPanelEnabled(panel_3d_options, true);
                typeAction = e.getActionCommand();
            }
        };
        _rb_snap_z = new JRadioButton("Snap to Z");
        _rb_snap_z
                .setToolTipText("When hitting \"snap\", one image will be added to the Z-Dimension of the current sequence.");
        _rb_snap_z.setActionCommand(TypeSnapZ);
        _rb_snap_z.addActionListener(radioListener);

        _rb_snap_t = new JRadioButton("Snap to T");
        _rb_snap_t
                .setToolTipText("When hitting \"snap\", slices will be added to the T-Dimension of the current sequence.");
        _rb_snap_t.setActionCommand(TypeSnapT);
        _rb_snap_t.addActionListener(radioListener);

        _rb_snap_c = new JRadioButton("Snap to C");
        _rb_snap_c
                .setToolTipText("When hitting \"snap\", slices will be added as a new channel of the current sequence.");
        _rb_snap_c.setActionCommand(TypeSnapC);
        _rb_snap_c.addActionListener(radioListener);

        ButtonGroup group = new ButtonGroup();
        group.add(_rb_snap_z);
        group.add(_rb_snap_t);
        group.add(_rb_snap_c);

        JPanel panel_snap_mode = GuiUtil.generatePanel("Snap options");
        panel_snap_mode.setLayout(new GridLayout(3, 1));
        panel_snap_mode.add(_rb_snap_z);
        panel_snap_mode.add(_rb_snap_t);
        panel_snap_mode.add(_rb_snap_c);

        // ----------------
        // SLICES OPTIONS
        // ---------------
        panel_3d_options = GuiUtil.generatePanel("Slices options");
        panel_3d_options.setLayout(new GridLayout(2, 2));

        final SpinnerNumberModel model_slices = new SpinnerNumberModel(10, 1, 1000, 1);
        final JSpinner _spin_slices = new JSpinner(model_slices);
        model_slices.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent changeevent)
            {
                _slices = model_slices.getNumber().intValue();
            }
        });

        final JTextField _tf_interval = new JTextField("1.0", 4);
        _tf_interval.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent keyevent)
            {
                if (keyevent.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    try
                    {
                        _interval_ = Double.valueOf(_tf_interval.getText()).doubleValue();
                        if (_interval_ < 1)
                        {
                            _interval_ = 1;
                            new FailedAnnounceFrame("Interval muste be > or = than 1", 5);
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        new FailedAnnounceFrame("Please enter a number for interval !", 5);
                    }
                }
            }
        });

        panel_3d_options.add(new JLabel("Slices Count: "));
        panel_3d_options.add(_spin_slices);
        panel_3d_options.add(new JLabel("Interval (µm): "));
        panel_3d_options.add(_tf_interval);

        // use doCLick() and not setEnabled in order to execute the action listener
        // 'radioListener'
        _rb_snap_z.doClick();

        // Creation of the panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(panel_info, BorderLayout.NORTH);
        mainPanel.add(panel_snap_mode, BorderLayout.EAST);
        mainPanel.add(panel_3d_options, BorderLayout.CENTER);
        mainPanel.add(panel_buttons, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);

        addToDesktopPane();
        center();
        pack();

        // fix minimum size
        setMinimumSizeInternal(getSize());
        setMinimumSizeExternal(getSize());

        requestFocus();
    }

    public void shutdown()
    {
        Icy.getMainInterface().removeActiveSequenceListener(this);
        Icy.getMainInterface().removeGlobalSequenceListener(this);
        StageMover.removeListener(this);
        dispose();
    }

    void setPanelEnabled(JPanel panel, boolean enabled)
    {
        panel.setEnabled(enabled);
        for (Component c : panel.getComponents())
        {
            if (c instanceof JPanel)
                setPanelEnabled((JPanel) c, enabled);
            else
                c.setEnabled(enabled);
        }
    }

    private void setCalendarNameToSequence(Sequence s)
    {
        Calendar c = Calendar.getInstance();
        s.setName(DateFormat.getDateInstance().format(c.getTime()) + " "
                + DateFormat.getTimeInstance().format(c.getTime()));
    }

    /**
     * Capture the stack of images according to the parameters.
     * 
     * @return Returns a sequence containing the acquired stack.
     */
    private Sequence captureStacks()
    {
        final Sequence result = new Sequence();

        plugin.showProgressBar(true);
        result.beginUpdate();
        try
        {
            if (typeAction.equals(TypeSnapZ) || _slices == 1)
            {
                result.addImage(ImageGetter.snapIcyImage());
                return result;
            }

            for (int z = 0; z < _slices; ++z)
            {
                StageMover.moveZRelative(_interval_);

                result.addImage(ImageGetter.snapIcyImage());
                double progress = z / _slices * 100;
                plugin.notifyProgress((int) progress);
            }

            StageMover.moveZAbsolute(initialPos);
        }
        catch (Exception e)
        {
            IcyExceptionHandler.handleException(e, true);
        }
        finally
        {
            result.endUpdate();
            plugin.removeProgressBar();
        }

        return result;
    }

    void createNewSequence()
    {
        final Sequence result = captureStacks();

        setCalendarNameToSequence(result);
        Icy.getMainInterface().addSequence(result);
    }

    void addToActiveSequence()
    {
        if (activeSequence == null)
            return;

        if (typeAction.equals(TypeSnapZ))
        {
            try
            {
                activeSequence.addImage(ImageGetter.snapIcyImage());
            }
            catch (IllegalArgumentException e)
            {
                new FailedAnnounceFrame("This sequence is not compatible");
            }
            catch (Exception e)
            {
                IcyExceptionHandler.handleException(e, true);
            }
        }
        else
        {
            Sequence seq = captureStacks();

            if (typeAction.equals(TypeSnapT))
            {
                // SNAP T
                final int t = activeSequence.getSizeT();
                int z = 0;

                activeSequence.beginUpdate();
                try
                {
                    for (IcyBufferedImage img : seq.getAllImage())
                        activeSequence.setImage(t, z++, img);
                }
                catch (IllegalArgumentException e)
                {
                    MessageDialog.showDialog("Error with snapping", "Image not compatible.");
                }
                finally
                {
                    activeSequence.endUpdate();
                }
            }
            else
            {
                // SNAP C
                if (_slices != activeSequence.getSizeZ())
                {
                    MessageDialog
                            .showDialog("Error number of stacks",
                                    "The number of stacks of the snap does not correspond to the number of stacks in the current sequence.");
                    return;
                }

                // get channel concatenation
                final Sequence tmp = SequenceUtil.concatC(new Sequence[] {activeSequence, seq});

                // apply it on activeSequence
                activeSequence.beginUpdate();
                try
                {
                    activeSequence.removeAllImages();
                    for (int t = 0; t < tmp.getSizeT(); t++)
                        for (int z = 0; z < tmp.getSizeZ(); z++)
                            activeSequence.setImage(t, z, tmp.getImage(t, z));
                }
                finally
                {
                    activeSequence.endUpdate();
                }
            }
        }
    }

    @Override
    public void onStagePositionChanged(String zStage, double z)
    {
        currentStagePosition.setText(StringUtil.toString(z, 3) + " µm");
    }

    @Override
    public void onStagePositionChangedRelative(String zStage, double z)
    {
    }

    @Override
    public void onXYStagePositionChanged(String XYStage, double x, double y)
    {
    }

    @Override
    public void onXYStagePositionChangedRelative(String XYStage, double x, double y)
    {
    }

    @Override
    public void sequenceOpened(Sequence sequence)
    {
    }

    @Override
    public void sequenceClosed(Sequence sequence)
    {
        if (Icy.getMainInterface().getSequences().size() == 0 || sequence.equals(activeSequence))
        {
            _btn_lock_sequence.setIcon(new IcyIcon(ResourceUtil.ICON_LOCK_OPEN));
            _btn_lock_sequence.setText("Lock the current working sequence.");
            activeSequence = null;
            currentWorkingSequence.setText("null");
            _btn_snap.setEnabled(false);
            _btn_lock_sequence.setEnabled(false);
        }
    }

    @Override
    public void sequenceActivated(Sequence sequence)
    {
        if ((lockActiveSequence && activeSequence != null) || Icy.getMainInterface().getSequences().size() == 0)
            return;

        if (lockActiveSequence && activeSequence == null)
            lockActiveSequence = false;

        activeSequence = sequence;
        if (sequence != null)
            currentWorkingSequence.setText(activeSequence.toString());

        _btn_snap.setEnabled(true);
        _btn_lock_sequence.setEnabled(true);
    }

    @Override
    public void sequenceDeactivated(Sequence sequence)
    {
    }

    @Override
    public void activeSequenceChanged(SequenceEvent event)
    {
    }
}