package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.MethodDeclaration;

public class MethodVisitor extends JavaVisitor
{
    @Override
    public void visit(MethodDeclaration n, Object arg)
    {
        list.add(n);
    }
}
