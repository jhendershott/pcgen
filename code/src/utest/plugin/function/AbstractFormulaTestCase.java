/*
 * Copyright 2014 (C) Tom Parker <thpr@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package plugin.function;

import java.util.List;

import junit.framework.TestCase;
import pcgen.base.formatmanager.FormatUtilities;
import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.EvaluationManager;
import pcgen.base.formula.base.FormulaManager;
import pcgen.base.formula.base.FormulaSemantics;
import pcgen.base.formula.base.FunctionLibrary;
import pcgen.base.formula.base.LegalScope;
import pcgen.base.formula.base.LegalScopeLibrary;
import pcgen.base.formula.base.OperatorLibrary;
import pcgen.base.formula.base.ScopeInstance;
import pcgen.base.formula.base.VariableID;
import pcgen.base.formula.base.VariableLibrary;
import pcgen.base.formula.base.WriteableVariableStore;
import pcgen.base.formula.inst.ScopeInstanceFactory;
import pcgen.base.formula.parse.SimpleNode;
import pcgen.base.formula.visitor.DependencyVisitor;
import pcgen.base.formula.visitor.EvaluateVisitor;
import pcgen.base.formula.visitor.SemanticsVisitor;
import pcgen.base.formula.visitor.StaticVisitor;
import pcgen.base.solver.IndividualSetup;
import pcgen.base.solver.Modifier;
import pcgen.base.solver.SplitFormulaSetup;
import pcgen.base.util.FormatManager;
import pcgen.cdom.formula.ManagerKey;
import pcgen.cdom.formula.MonitorableVariableStore;
import pcgen.cdom.formula.scope.LegalScopeUtilities;
import pcgen.rules.context.LoadContext;

public abstract class AbstractFormulaTestCase extends TestCase
{

	protected FormatManager<Number> numberManager =
			FormatUtilities.NUMBER_MANAGER;
	protected FormatManager<String> stringManager =
			FormatUtilities.STRING_MANAGER;

	private SplitFormulaSetup setup;
	private IndividualSetup localSetup;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		setup = new SplitFormulaSetup();
		LegalScopeUtilities.loadLegalScopeLibrary(setup.getLegalScopeLibrary());
		localSetup = new IndividualSetup(setup, "Global", new MonitorableVariableStore());
		setup.getSolverFactory().addSolverFormat(Number.class, new Modifier()
		{

			@Override
			public Object process(EvaluationManager manager)
			{
				return 0;
			}

			@Override
			public void getDependencies(DependencyManager fdm)
			{
			}

			@Override
			public long getPriority()
			{
				return 0;
			}

			@Override
			public Class getVariableFormat()
			{
				return Number.class;
			}

			@Override
			public String getIdentification()
			{
				return "SET";
			}

			@Override
			public String getInstructions()
			{
				return "0";
			}
		});
		setup.getSolverFactory().addSolverFormat(String.class, new Modifier()
		{

			@Override
			public Object process(EvaluationManager manager)
			{
				return "";
			}

			@Override
			public void getDependencies(DependencyManager fdm)
			{
			}

			@Override
			public long getPriority()
			{
				return 0;
			}

			@Override
			public Class getVariableFormat()
			{
				return String.class;
			}

			@Override
			public String getIdentification()
			{
				return "SET";
			}

			@Override
			public String getInstructions()
			{
				return "";
			}
		});
	}

	public void isValid(String formula, SimpleNode node,
		FormatManager<?> formatManager, Class<?> assertedFormat)
	{
		SemanticsVisitor semanticsVisitor = new SemanticsVisitor();
		FormulaSemantics semantics = generateFormulaSemantics(
			localSetup.getFormulaManager(), getGlobalScope(), assertedFormat);
		semanticsVisitor.visit(node, semantics);
		if (!semantics.isValid())
		{
			TestCase.fail("Expected Valid Formula: " + formula
				+ " but was told: " + semantics.getReport());
		}
	}

	protected FormulaSemantics generateFormulaSemantics(
		FormulaManager formulaManager, LegalScope globalScope,
		Class<?> assertedFormat)
	{
		return new FormulaSemantics()
			.getWith(FormulaSemantics.FMANAGER, formulaManager)
			.getWith(FormulaSemantics.SCOPE, globalScope)
			.getWith(FormulaSemantics.ASSERTED, assertedFormat);
	}

	public void isStatic(String formula, SimpleNode node, boolean b)
	{
		StaticVisitor staticVisitor =
				new StaticVisitor(localSetup.getFormulaManager().get(FormulaManager.FUNCTION));
		boolean isStat =
				((Boolean) staticVisitor.visit(node, null)).booleanValue();
		if (isStat != b)
		{
			TestCase.fail("Expected Static (" + b + ") Formula: " + formula);
		}
	}

	public void evaluatesTo(String formula, SimpleNode node, Object valueOf,
		LoadContext context)
	{
		EvaluationManager manager = generateManager(context);
		performEvaluation(formula, node, valueOf, manager);
	}

	public void performEvaluation(String formula, SimpleNode node,
		Object valueOf, EvaluationManager manager)
	{
		Object result = new EvaluateVisitor().visit(node, manager);
		if (result.equals(valueOf))
		{
			return;
		}
		//Try ints as double as well just in case (temporary)
		if (valueOf instanceof Integer)
		{
			if (result.equals(valueOf))
			{
				return;
			}
		}
		//Give Doubles a bit of fuzz
		else if (valueOf instanceof Double)
		{
			if (TestUtilities.doubleEqual(((Double) valueOf).doubleValue(),
				((Number) result).doubleValue(), TestUtilities.SMALL_ERROR))
			{
				return;
			}
		}
		TestCase.fail("Expected " + valueOf.getClass().getSimpleName() + " ("
			+ valueOf + ") for Formula: " + formula + ", was " + result + " ("
			+ result.getClass().getSimpleName() + ")");
	}

	public EvaluationManager generateManager(LoadContext context)
	{
		return new EvaluationManager()
			.getWith(EvaluationManager.FMANAGER, localSetup.getFormulaManager())
			.getWith(EvaluationManager.INSTANCE, localSetup.getGlobalScopeInst())
			.getWith(EvaluationManager.ASSERTED, Number.class)
			.getWith(ManagerKey.CONTEXT, context);
	}

	protected void isNotValid(String formula, SimpleNode node,
		FormatManager<?> formatManager, Class<?> assertedFormat)
	{
		SemanticsVisitor semanticsVisitor = new SemanticsVisitor();
		FormulaSemantics semantics = generateFormulaSemantics(
			localSetup.getFormulaManager(), getGlobalScope(), assertedFormat);
		semanticsVisitor.visit(node, semantics);
		if (semantics.isValid())
		{
			TestCase.fail(
				"Expected Invalid Formula: " + formula + " but was valid");
		}
	}

	protected List<VariableID<?>> getVariables(SimpleNode node)
	{
		DependencyManager fdm = generateDependencyManager(getFormulaManager(),
			getGlobalScopeInst(), null);
		new DependencyVisitor().visit(node, fdm);
		return fdm.getVariables();
	}

	private DependencyManager generateDependencyManager(
		FormulaManager formulaManager, ScopeInstance globalScopeInst,
		Class<?> assertedFormat)
	{
		return new DependencyManager()
			.getWith(DependencyManager.FMANAGER, formulaManager)
			.getWith(DependencyManager.INSTANCE, globalScopeInst)
			.getWith(DependencyManager.ASSERTED, assertedFormat);
	}

	protected VariableID<Number> getVariable(String formula)
	{
		VariableLibrary variableLibrary = getVariableLibrary();
		variableLibrary.assertLegalVariableID(formula,
			localSetup.getGlobalScopeInst().getLegalScope(), numberManager);
		return (VariableID<Number>) variableLibrary
			.getVariableID(localSetup.getGlobalScopeInst(), formula);
	}

	protected VariableID<Boolean> getBooleanVariable(String formula)
	{
		VariableLibrary variableLibrary = getVariableLibrary();
		variableLibrary.assertLegalVariableID(formula,
			localSetup.getGlobalScopeInst().getLegalScope(),
			FormatUtilities.BOOLEAN_MANAGER);
		return (VariableID<Boolean>) variableLibrary
			.getVariableID(localSetup.getGlobalScopeInst(), formula);
	}

	protected FunctionLibrary getFunctionLibrary()
	{
		return localSetup.getFormulaManager().get(FormulaManager.FUNCTION);
	}

	protected OperatorLibrary getOperatorLibrary()
	{
		return localSetup.getFormulaManager().getOperatorLibrary();
	}

	protected VariableLibrary getVariableLibrary()
	{
		return localSetup.getFormulaManager().getFactory();
	}

	protected WriteableVariableStore getVariableStore()
	{
		return (WriteableVariableStore) localSetup.getFormulaManager()
			.getResolver();
	}

	protected LegalScope getGlobalScope()
	{
		return localSetup.getGlobalScopeInst().getLegalScope();
	}

	protected ScopeInstance getGlobalScopeInst()
	{
		return localSetup.getGlobalScopeInst();
	}

	protected FormulaManager getFormulaManager()
	{
		return localSetup.getFormulaManager();
	}

	protected LegalScopeLibrary getScopeLibrary()
	{
		return setup.getLegalScopeLibrary();
	}

	protected ScopeInstanceFactory getInstanceFactory()
	{
		return localSetup.getInstanceFactory();
	}
}
