package plugins.tprovoost.Microscopy.MicroscopeSnapper;

import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

public class MicroscopeSnapperPlugin extends MicroscopePlugin
{
    // static instance
    static MicroSnapperFrame instance;

    public MicroscopeSnapperPlugin()
    {
        super();

        if (instance == null)
            instance = new MicroSnapperFrame(this);
    }

    @Override
    public void start()
    {
        if (instance != null)
            instance.toFront();
    }

    @Override
    public void shutdown()
    {
        super.shutdown();

        if (instance != null)
            instance.shutdown();
        instance = null;
    }
}
