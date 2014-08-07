package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.ConstructorDeclaration;

public class ConstructorVisitor extends JavaVisitor
{
    @Override
    public void visit(ConstructorDeclaration n, Object arg)
    {
        list.add(n);
    }
}
