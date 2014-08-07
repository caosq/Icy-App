package plugins.tprovoost.scripteditor.search;

import icy.plugin.PluginDescriptor;
import icy.util.StringUtil;

import javax.swing.ImageIcon;

import org.pushingpixels.flamingo.api.common.RichTooltip;

public class ScriptRichToolTip extends RichTooltip
{

    public ScriptRichToolTip(ScriptDescriptor script)
    {
        final String name = script.getName();
        final String description = script.getDescription();
        final String website = script.getUrl();
        final String author = script.getAuthor();
        final ImageIcon plugIcon = script.getIcon();
        // final Image plugImg = script.getImage();

        setTitle(name);
        if (plugIcon != PluginDescriptor.DEFAULT_ICON)
            setMainImage(plugIcon.getImage());

        if (!StringUtil.isEmpty(description))
        {
            for (String str : description.split("\n"))
                if (!StringUtil.isEmpty(str))
                    addDescriptionSection(str);
        }
        if (!StringUtil.isEmpty(website))
            addDescriptionSection(website);
        if (!StringUtil.isEmpty(author))
            addDescriptionSection(author);

        // if (plugImg != PluginDescriptor.DEFAULT_IMAGE)
        // setFooterImage(plugin.getImage());
    }

}
