package plugins.tprovoost.scripteditor.scriptingconsole;

import icy.gui.frame.IcyFrame;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngine;
import plugins.tprovoost.scripteditor.scriptinghandlers.ScriptEngineHandler;

// FIXME
public class BindingsScriptFrame extends IcyFrame
{
	// Warning! The BindingsFrame keeps a reference on the engine, and is a singleton
	// It means that the referenced engine will not be garbage-collected unless
	// it is set as null
	private ScriptEngine engine;
	private JButton btnFreeVar;
	private JTable listVariables;
	private BindingsTableModel model;
	private static BindingsScriptFrame singleton = new BindingsScriptFrame();

	protected BindingsScriptFrame()
	{
		super("Engine Bindings", true, true, true);

		JPanel mainPanel = new JPanel();

		btnFreeVar = new JButton("Free Var");
		btnFreeVar.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				int selectedRow = listVariables.getSelectedRow();
				Object o = listVariables.getValueAt(selectedRow, 0);
				Object val = listVariables.getValueAt(selectedRow, 1);
				if (val instanceof NativeArray)
				{
					for (int i = 0; i < ((NativeArray) val).getLength(); ++i)
					{
						((NativeArray) val).delete(i);
					}
				} else if (val instanceof IdScriptableObject || val instanceof NativeJavaObject)
				{
					Scriptable scope = ((NativeJavaObject) val).getParentScope();
					scope.put((String) o, scope, null);
				}
				engine.deleteBinding((String) o);
				update();
			}
		});
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				update();
			}
		});

		model = new BindingsTableModel();
		listVariables = new JTable(model);

		mainPanel.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(listVariables);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(scroll, BorderLayout.CENTER);
		JPanel panelSouth = new JPanel();
		panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));
		panelSouth.add(Box.createHorizontalGlue());
		panelSouth.add(btnFreeVar);
		panelSouth.add(Box.createHorizontalGlue());
		panelSouth.add(btnRefresh);
		panelSouth.add(Box.createHorizontalGlue());
		mainPanel.add(panelSouth, BorderLayout.SOUTH);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setContentPane(mainPanel);
		addToMainDesktopPane();
		setSize(200, 700);
	}

	public void setEngine(ScriptEngine engine)
	{
		this.engine = engine;
	}

	public void update()
	{
		if (engine != null)
		{
			String languageName = engine.getName();
			ScriptEngine engine = ScriptEngineHandler.getEngine(languageName);
			if (engine != this.engine)
				this.engine = engine;
			ThreadUtil.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					model.fireTableDataChanged();
					listVariables.repaint();
				}
			});
		}
	}

	private class BindingsTableModel extends AbstractTableModel
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String getColumnName(int column)
		{
			if (column == 0)
			{
				return "Binding";
			} else if (column == 1)
			{
				return "Value";
			} else
			{
				return "";
			}
		}

		@Override
		public int getColumnCount()
		{
			return 2;
		}

		@Override
		public int getRowCount()
		{
			if (engine != null)
				return engine.getBindings().values().size();
			return 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Set<String> keyset = engine.getBindings().keySet();
			if (rowIndex >= 0 && rowIndex < keyset.size())
			{
				if (engine == null)
					return null;
				if (columnIndex == 0)
				{
					return keyset.toArray()[rowIndex];
				} else
				{
					Object o = engine.getBindings().values().toArray()[rowIndex];
					// Class<?> clazz = o.getClass();
					return o;
				}
			}
			return null;
		}
	}

	public static BindingsScriptFrame getInstance()
	{
		return singleton;
	}
}
