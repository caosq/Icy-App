package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public abstract class JavaVisitor extends VoidVisitorAdapter
{
    protected ArrayList<BodyDeclaration> list = new ArrayList<BodyDeclaration>();

    public ArrayList<BodyDeclaration> getList()
    {
        return list;
    }
}