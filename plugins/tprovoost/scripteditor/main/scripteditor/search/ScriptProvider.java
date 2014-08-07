package plugins.tprovoost.scripteditor.search;

import icy.gui.frame.progress.DownloadFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.network.NetworkUtil;
import icy.network.URLUtil;
import icy.search.SearchResult;
import icy.search.SearchResultConsumer;
import icy.search.SearchResultProducer;
import icy.system.SystemUtil;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;
import icy.util.XMLUtil;

import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import org.pushingpixels.flamingo.api.common.RichTooltip;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import plugins.tprovoost.scripteditor.main.ScriptEditorPlugin;

public class ScriptProvider extends SearchResultProducer
{

    private static final String SEARCH_URL = "http://bioimageanalysis.org/icy/search/search.php?search=";

    private static final String ID_SEARCH_RESULT = "searchresult";
    private static final String ID_SCRIPT = "script";
    private static final String ID_TEXT = "string";

    private final long REQUEST_INTERVAL = 400;

    @Override
    public String getName()
    {
        return "Scripts";
    }

    @Override
    public String getTooltipText()
    {
        return "Result(s) from online scripts";
    }

    @Override
    public void doSearch(String[] words, SearchResultConsumer consumer)
    {
        String request = SEARCH_URL;

        if (words.length > 0)
            request += words[0].replace("+", "%2B").replace("&", "%26").replace("@", "%40").replace("<", "%3C")
                    .replace(">", "%3E");
        if (words.length > 1)
        {
            for (int i = 1; i < words.length; i++)
                request += "%20"
                        + words[i].replace("+", "%2B").replace("&", "%26").replace("@", "%40").replace("<", "%3C")
                                .replace(">", "%3E");
        }

        final long startTime = System.currentTimeMillis();

        // wait interval elapsed before sending request (avoid website request
        // spam)
        while ((System.currentTimeMillis() - startTime) < REQUEST_INTERVAL)
        {
            ThreadUtil.sleep(10);
            // abort
            if (hasWaitingSearch())
                return;
        }

        Document doc = null;
        int retry = 0;

        // let's 10 try to get the result
        while ((doc == null) && (retry < 10))
        {
            // we use an online request as website can search in plugin
            // documention
            doc = XMLUtil.loadDocument(URLUtil.getURL(request), false);

            // abort
            if (hasWaitingSearch())
                return;

            // error ? --> wait a bit before retry
            if (doc == null)
                ThreadUtil.sleep(100);

            retry++;
        }

        // can't get result from website --> exit
        if (doc == null)
            return;

        if (hasWaitingSearch())
        {
            return;
        }

        // get online result node
        final Element resultElement = XMLUtil.getElement(doc.getDocumentElement(), ID_SEARCH_RESULT);

        if (resultElement == null)
            return;

        final ArrayList<SearchResult> tmpResults = new ArrayList<SearchResult>();

        for (Element script : XMLUtil.getElements(resultElement, ID_SCRIPT))
        {
            // abort
            if (getClass().getClassLoader() != SystemUtil.getSystemClassLoader() && hasWaitingSearch())
                return;

            final SearchResult result = getResult(script, words);

            if (result != null)
                tmpResults.add(result);
        }

        results = tmpResults;
        consumer.resultsChanged(this);
    }

    private OnlineScriptResult getResult(Element scriptNode, String words[])
    {
        final String text = XMLUtil.getElementValue(scriptNode, ID_TEXT, "");

        ScriptDescriptor script = new ScriptDescriptor(scriptNode);

        return new OnlineScriptResult(this, script, text, words);
    }

    private class OnlineScriptResult extends SearchResult
    {

        private ScriptDescriptor script;
        private String description;

        public OnlineScriptResult(ScriptProvider scriptProvider, ScriptDescriptor script, String text,
                String[] searchWords)
        {
            super(scriptProvider);

            this.script = script;

            int wi = 0;
            description = "";
            while (StringUtil.isEmpty(description) && (wi < searchWords.length))
            {
                // no more than 80 characters...
                description = StringUtil.trunc(text, searchWords[wi], 80);
                wi++;
            }

            if (!StringUtil.isEmpty(description))
            {
                // remove carriage return
                description = description.replace("\n", "");

                // highlight search keywords (only for more than 2 characters
                // search)
                if ((searchWords.length > 1) || (searchWords[0].length() > 2))
                {
                    // highlight search keywords
                    for (String word : searchWords)
                        description = StringUtil.htmlBoldSubstring(description, word, true);
                }
            }
        }

        @Override
        public String getTitle()
        {
            return script.getName();
        }

        @Override
        public Image getImage()
        {
            ImageIcon icon = script.getIcon();
            if (icon != null)
                return icon.getImage();

            return null;
        }

        @Override
        public String getDescription()
        {
            return description;
        }

        @Override
        public String getTooltip()
        {
            return "Left click: Open   -   Right click: Online documentation";
        }

        @Override
        public void execute()
        {
            ThreadUtil.bgRun(new Runnable()
            {
                @Override
                public void run()
                {
                    final String name = script.getName() + " v" + script.getVersion() + "." + script.getExtension();
                    final DownloadFrame frameDownload = new DownloadFrame(name);
                    try
                    {
                        byte[] data = NetworkUtil.download(new URL(script.getFileUrl()), frameDownload, true);

                        if (data != null)
                        {
                            final String s = new String(data);
                            ThreadUtil.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    ScriptEditorPlugin.openInScriptEditor(s, name);
                                }
                            });
                        }
                    }
                    catch (MalformedURLException e1)
                    {
                        new FailedAnnounceFrame("Could not load the script :" + e1.getMessage());
                    }
                    finally
                    {
                        frameDownload.close();
                    }
                }
            });
        }

        @Override
        public void executeAlternate()
        {
            NetworkUtil.openBrowser(script.getUrl());
        }

        @Override
        public RichTooltip getRichToolTip()
        {
            return new ScriptRichToolTip(script);
        }
    }
}
