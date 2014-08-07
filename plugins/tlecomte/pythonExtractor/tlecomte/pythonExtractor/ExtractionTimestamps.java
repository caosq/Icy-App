package plugins.tlecomte.pythonExtractor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.plugin.PluginDescriptor;
import icy.util.XMLUtil;

public class ExtractionTimestamps implements XMLPersistent {
	
	Hashtable<String, Long> timestamps;
	
	static final String ID_PLUGINS = "plugins";
	static final String ID_PLUGIN = "plugin";
	static final String ID_CLASSNAME = "className";
	static final String ID_TIMESTAMP = "timestamp";
	
	ExtractionTimestamps() {
		timestamps = new Hashtable<String, Long>();
	}
	
	public Long getTimestamp(PluginDescriptor plugin) {
		Long timestamp = timestamps.get(plugin.getClassName());
		if (timestamp == null) {
			timestamp = new Long(0);
		}
		return timestamp;
	}
    
	public void setTimestamp(PluginDescriptor plugin, Long timestamp) {
		timestamps.put(plugin.getClassName(), timestamp);
	}

	@Override
	public boolean loadFromXML(Node node) {
        if (node == null)
            return false;

        final Node pluginsNode = XMLUtil.getElement(node, ID_PLUGINS);

        if (pluginsNode != null)
        {
            final ArrayList<Node> pluginNodes = XMLUtil.getChildren(pluginsNode, ID_PLUGIN);

            for (Node n : pluginNodes)
            {
            	String pluginName = XMLUtil.getElementValue(n, ID_CLASSNAME, "noClassName");
            	Long timestamp = XMLUtil.getElementLongValue(n, ID_TIMESTAMP, 0);
            	timestamps.put(pluginName, timestamp);
            }
        }

        return true;
	}

	@Override
	public boolean saveToXML(Node node) {
        if (node == null)
            return false;

        final Element pluginsNode = XMLUtil.setElement(node, ID_PLUGINS);

        if (pluginsNode != null)
        {
            XMLUtil.removeAllChildren(pluginsNode);
            
            Enumeration<String> enumKey = timestamps.keys();
            while(enumKey.hasMoreElements()) {
                String key = enumKey.nextElement();
                Long timestamp = timestamps.get(key);
                Element n = XMLUtil.addElement(pluginsNode, ID_PLUGIN);
                XMLUtil.setElementValue(n, ID_CLASSNAME, key);
                XMLUtil.setElementLongValue(n, ID_TIMESTAMP, timestamp);
            }
        }

        return true;
	}

}
