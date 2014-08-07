package plugins.tprovoost.scripteditor.search;

import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginSearchProvider;
import icy.search.SearchResultProducer;

public class OnlineScriptProvider extends Plugin implements PluginSearchProvider
{
    @Override
    public Class<? extends SearchResultProducer> getSearchProviderClass()
    {
        return ScriptProvider.class;
    }
}
