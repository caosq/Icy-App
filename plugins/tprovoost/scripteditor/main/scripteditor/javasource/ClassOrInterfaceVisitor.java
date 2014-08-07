package plugins.tprovoost.scripteditor.javasource;

import japa.parser.ast.body.ClassOrInterfaceDeclaration;

public class ClassOrInterfaceVisitor extends JavaVisitor
{
    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg)
    {
        list.add(n);
    }

}
