package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.EnumDeclaration;

public class EnumVisitor extends JavaVisitor
{
    @Override
    public void visit(EnumDeclaration n, Object arg)
    {
        list.add(n);
    }
}
