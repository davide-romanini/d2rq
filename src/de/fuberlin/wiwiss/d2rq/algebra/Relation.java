package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO Add uniqueConstraints()
 * TODO Explicitly list tables!!!
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Relation.java,v 1.11 2008/04/27 22:42:36 cyganiak Exp $
 */
public abstract class Relation implements RelationalOperators {

	public static Relation createSimpleRelation(
			ConnectedDB database, Attribute[] attributes) {
		return new RelationImpl(database, AliasMap.NO_ALIASES, Expression.TRUE,
				Collections.EMPTY_SET, new HashSet(Arrays.asList(attributes)), false);
	}
	
	public static Relation EMPTY = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.FALSE; }
		public Set projections() { return Collections.EMPTY_SET; }
		public Relation select(Expression condition) { return this; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public Relation project(Set projectionSpecs) { return this; }
		public boolean isUnique() { return true; }
		public String toString() { return "Relation.EMPTY"; }
	};
	public static Relation TRUE = new Relation() {
		public ConnectedDB database() { return null; }
		public AliasMap aliases() { return AliasMap.NO_ALIASES; }
		public Set joinConditions() { return Collections.EMPTY_SET; }
		public Expression condition() { return Expression.TRUE; }
		public Set projections() { return Collections.EMPTY_SET; }
		// TODO This is dangerous; TRUE can remain TRUE or become EMPTY upon select()
		public Relation select(Expression condition) { return Relation.TRUE; }
		public Relation renameColumns(ColumnRenamer renamer) { return this; }
		public Relation project(Set projectionSpecs) { return this; }
		public boolean isUnique() { return true; }
		public String toString() { return "Relation.TRUE"; }
	};
	
	public abstract ConnectedDB database();
	
	/**
	 * The tables that are used to set up this relation, both in
	 * their aliased form, and with their original physical names.
	 * @return All table aliases required by this relation
	 */
	public abstract AliasMap aliases();
	
	/**
	 * Returns the join conditions that must hold between the tables
	 * in the relation.
	 * @return A set of {@link Join}s 
	 */
	public abstract Set joinConditions();

	/**
	 * An expression that must be satisfied for all tuples in the
	 * relation.
	 * @return An expression; {@link Expression#TRUE} indicates no condition
	 */
	public abstract Expression condition();
	
	/**
	 * The attributes or expressions that the relation is projected to.
	 * @return A set of {@link ProjectionSpec}s
	 */
	public abstract Set projections();
	
	public abstract boolean isUnique();
	
	public Set allKnownAttributes() {
		Set results = new HashSet();
		results.addAll(condition().attributes());
		Iterator it = joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			results.addAll(join.attributes1());
			results.addAll(join.attributes2());
		}
		it = projections().iterator();
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			results.addAll(projection.requiredAttributes());
		}
		return results;
	}

	public Set tables() {
		Set results = new HashSet();
		Iterator it = allKnownAttributes().iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			results.add(attribute.relationName());
		}
		return results;
	}
}