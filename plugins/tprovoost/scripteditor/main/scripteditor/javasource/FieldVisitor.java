package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.FieldDeclaration;

public class FieldVisitor extends JavaVisitor
{
    @Override
    public void visit(FieldDeclaration n, Object arg)
    {
        list.add(n);
    }
}