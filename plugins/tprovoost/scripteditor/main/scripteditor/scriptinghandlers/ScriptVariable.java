package plugins.tprovoost.scripteditor.scriptinghandlers;

import java.util.ArrayList;

public class ScriptVariable
{
    private ArrayList<ScriptVariableScope> variableScopes = new ArrayList<ScriptVariableScope>();

    public ScriptVariable()
    {
    }

    public ScriptVariable(VariableType returnType)
    {
        addType(0, returnType);
    }

    public void addType(int offsetBegin, VariableType returnType)
    {
        addType(offsetBegin, -1, returnType);
    }

    public void addType(int offsetBegin, int offsetEnd, VariableType returnType)
    {
        if (!variableScopes.isEmpty())
        {
            ScriptVariableScope lastScope = variableScopes.get(variableScopes.size() - 1);
            if (lastScope.endScopeOffset == -1)
                lastScope.endScopeOffset = offsetBegin - 1;
        }
        variableScopes.add(new ScriptVariableScope(offsetBegin, offsetEnd, returnType));
    }

    public VariableType getVariableClassType(int offset)
    {
        for (ScriptVariableScope svc : variableScopes)
        {
            VariableType type = svc.getType(offset);
            if (type != null)
                return type;
        }
        return null;
    }

    public VariableType getVariableLastClassType()
    {
        if (!variableScopes.isEmpty())
        {
            return variableScopes.get(variableScopes.size() - 1).type;
        }
        return null;
    }

    public class ScriptVariableScope
    {
        private int declarationOffset;
        private int endScopeOffset;
        private VariableType type;

        public ScriptVariableScope(int declarationOffset, int endScopeOffset, Class<?> type)
        {
            this(declarationOffset, endScopeOffset, new VariableType(type));
        }

        public ScriptVariableScope(int declarationOffset, int endScopeOffset, VariableType type)
        {
            this.declarationOffset = declarationOffset;
            this.endScopeOffset = endScopeOffset;
            this.type = type;
            variableScopes.add(this);
        }

        public VariableType getType(int offset)
        {
            if (offset >= declarationOffset && (endScopeOffset == -1 || offset < endScopeOffset))
                return type;
            return null;
        }
        
        public int getDeclarationOffset()
		{
			return declarationOffset;
		}
        
        public int getEndScopeOffset()
		{
			return endScopeOffset;
		}
        
        public void setDeclarationOffset(int declarationOffset)
		{
			this.declarationOffset = declarationOffset;
		}
        
        public void setEndScopeOffset(int endScopeOffset)
		{
			this.endScopeOffset = endScopeOffset;
		}
    }
    
    public ArrayList<ScriptVariableScope> getVariableScopes()
	{
		return variableScopes;
	}

    public boolean isInScope(int offset)
    {
        for (ScriptVariableScope svc : variableScopes)
        {
            boolean inScope = offset >= svc.declarationOffset
                    && (svc.endScopeOffset == -1 || offset < svc.endScopeOffset);
            if (inScope)
                return true;
        }
        return false;
    }
}
