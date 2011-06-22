/*
 * This file is part of RDF Federator.
 * Copyright 2011 Olaf Goerlitz
 * 
 * RDF Federator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * RDF Federator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with RDF Federator.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * RDF Federator uses libraries from the OpenRDF Sesame Project licensed 
 * under the Aduna BSD-style license. 
 */
package de.uni_koblenz.west.federation.helpers;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Compare;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.QueryModelNode;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.UnaryTupleOperator;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

import de.uni_koblenz.west.federation.estimation.ModelEvaluator;
import de.uni_koblenz.west.federation.model.RemoteQuery;

/**
 * @author Olaf Goerlitz
 */
public class AnnotatingTreePrinter extends QueryModelVisitorBase<RuntimeException> {
	
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static final String indentString = "  ";

	private StringBuilder buffer;

	private int indentLevel = 0;
	
	private ModelEvaluator eval;
	private String evalLabel;
	
	public AnnotatingTreePrinter(ModelEvaluator eval) {
		this.buffer = new StringBuilder(256);
		this.eval = eval;
		this.evalLabel = eval.getName();
	}
	
	public static String print(QueryModelNode root) {
		return print(root, null);
	}
	
	public static String print(QueryModelNode root, ModelEvaluator eval) {
		AnnotatingTreePrinter printer = new AnnotatingTreePrinter(eval);
		root.visit(printer);
		return printer.toString();
	}
	
	private void addIndent() {
		for (int i = 0; i < indentLevel; i++) {
			buffer.append(indentString);
		}
	}
	
	@Override
	public void meet(Compare node) throws RuntimeException {
		node.getLeftArg().visit(this);
		buffer.append(" ").append(node.getOperator().getSymbol()).append(" ");
		node.getRightArg().visit(this);
	}

	@Override
	public void meet(Filter node) throws RuntimeException {
		addIndent();
		buffer.append("FILTER (");
		node.getCondition().visit(this);
		buffer.append(")").append(LINE_SEPARATOR);
		indentLevel++;
		node.getArg().visit(this);
		indentLevel--;
	}
	
	@Override
	public void meet(StatementPattern node) throws RuntimeException {
		addIndent();
		for (Var var : node.getVarList()) {
			var.visit(this);
			buffer.append(" ");
		}
		if (eval != null)
			buffer.append(" [").append(evalLabel).append(": ").append(eval.process(node)).append("]");
	}

	@Override
	public void meet(Var node) throws RuntimeException {
		if (node.hasValue()) {
			Value value = node.getValue();
			if (value instanceof URI)
				buffer.append("<").append(value).append(">");
			else
				buffer.append(node.getValue());
		} else
			buffer.append("?").append(node.getName());
	}

	@Override
	protected void meetBinaryTupleOperator(BinaryTupleOperator node)
			throws RuntimeException {
		addIndent();
		buffer.append(node.getSignature().toUpperCase());
		if (eval != null)
			buffer.append(" [").append(evalLabel).append(": ").append(eval.process(node)).append("]");
		
		indentLevel++;
		buffer.append(LINE_SEPARATOR);
		node.getLeftArg().visit(this);
		buffer.append(LINE_SEPARATOR);
		node.getRightArg().visit(this);
		indentLevel--;
	}
	
	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node)
			throws RuntimeException {
		if (node instanceof RemoteQuery) {
			meet((RemoteQuery) node);
		} else {
			super.meetUnaryTupleOperator(node);
		}
	}
	
	protected void meet(RemoteQuery node) {
		addIndent();
		buffer.append("###REMOTE QUERY### @").append(node.getSources()).append(LINE_SEPARATOR);
		node.getArg().visit(this);
	}

	@Override
	public String toString() {
		return this.buffer.toString();
	}

}
