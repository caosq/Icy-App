package plugins.tprovoost.scripteditor.completion;

import java.awt.Image;
import java.awt.image.BufferedImage;

import icy.image.ImageUtil;
import icy.plugin.PluginLoader;

import javax.swing.ImageIcon;
import javax.swing.JList;

import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.VariableCompletion;

import plugins.tprovoost.scripteditor.completion.types.BasicJavaClassCompletion;
import plugins.tprovoost.scripteditor.completion.types.NewInstanceCompletion;
import plugins.tprovoost.scripteditor.completion.types.ScriptFunctionCompletion;

public class IcyCompletionCellRenderer extends CompletionCellRenderer
{

    /** */
    private static final long serialVersionUID = 1L;

    // images
    public static final BufferedImage IMAGE_STATIC = ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/static_co.gif"));

    public static final BufferedImage IMAGE_METHOD = ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/methpub_obj.gif"));

    public static final BufferedImage IMAGE_DEPRECATED = ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/deprecated.gif"));

    // icons
    public static final ImageIcon ICON_METHOD = new ImageIcon(IMAGE_METHOD);

    public static final ImageIcon ICON_VARIABLES = new ImageIcon(ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/field_public_obj.gif")));

    public static final ImageIcon ICON_OTHER = new ImageIcon(ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/constant_co.gif")));

    public static final ImageIcon ICON_CLASS = new ImageIcon(ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/class_obj.gif")));

    public static final ImageIcon ICON_INSTERFACE = new ImageIcon(ImageUtil.load(PluginLoader
            .getResourceAsStream("plugins/tprovoost/scripteditor/resources/icons/int_obj.gif")));

    @Override
    protected void prepareForFunctionCompletion(JList list, FunctionCompletion fc, int index, boolean selected,
            boolean hasFocus)
    {
        if (fc instanceof ScriptFunctionCompletion)
        {
            ImageIcon icon = ICON_METHOD;
            BufferedImage img = null;
            if (((ScriptFunctionCompletion) fc).isStatic())
            {
                BufferedImage func2 = ImageUtil.getCopy(IMAGE_METHOD);
                func2.getGraphics().drawImage(IMAGE_STATIC, 0, 0, null);
                img = func2;
            }
            if (((ScriptFunctionCompletion) fc).getMethod().getAnnotation(Deprecated.class) != null)
            {
                if (img == null)
                    img = ImageUtil.getCopy(IMAGE_METHOD);
                img.getGraphics().drawImage(IMAGE_DEPRECATED, 0, 0, null);
            }
            if (img != null)
                setIcon(new ImageIcon(img));
            else
                setIcon(icon);
        }
        else if (fc instanceof NewInstanceCompletion)
        {
            setIcon(ICON_CLASS);
        }
        else
            setIcon(ICON_METHOD);
        super.prepareForFunctionCompletion(list, fc, index, selected, hasFocus);
    }

    @Override
    protected void prepareForVariableCompletion(JList list, VariableCompletion vc, int index, boolean selected,
            boolean hasFocus)
    {
        ImageIcon icon;
        if (vc instanceof BasicJavaClassCompletion)
        {
            Class<?> javaClass = ((BasicJavaClassCompletion) vc).getJavaClass();
            if (javaClass.isInterface())
                icon = ICON_INSTERFACE;
            else
                icon = ICON_CLASS;

            // test deprecation
            if (javaClass.getAnnotation(Deprecated.class) != null)
            {
                Image img = ImageUtil.getCopy(icon.getImage());
                img.getGraphics().drawImage(IMAGE_DEPRECATED, 0, 0, null);
                setIcon(new ImageIcon(img));
            }
            else
                setIcon(icon);
        }
        else
        {
            icon = ICON_VARIABLES;
            setIcon(icon);
        }

        super.prepareForVariableCompletion(list, vc, index, selected, hasFocus);
    }

    @Override
    protected void prepareForOtherCompletion(JList list, Completion c, int index, boolean selected, boolean hasFocus)
    {
        setIcon(ICON_OTHER);
        super.prepareForOtherCompletion(list, c, index, selected, hasFocus);
    }

}
