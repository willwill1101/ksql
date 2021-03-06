/**
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.confluent.ksql.parser;


import io.confluent.ksql.parser.tree.AliasedRelation;
import io.confluent.ksql.parser.tree.ArithmeticBinaryExpression;
import io.confluent.ksql.parser.tree.ArithmeticUnaryExpression;
import io.confluent.ksql.parser.tree.AstVisitor;
import io.confluent.ksql.parser.tree.BetweenPredicate;
import io.confluent.ksql.parser.tree.Cast;
import io.confluent.ksql.parser.tree.ComparisonExpression;
import io.confluent.ksql.parser.tree.CreateTableAsSelect;
import io.confluent.ksql.parser.tree.Delete;
import io.confluent.ksql.parser.tree.DereferenceExpression;
import io.confluent.ksql.parser.tree.Expression;
import io.confluent.ksql.parser.tree.Extract;
import io.confluent.ksql.parser.tree.FrameBound;
import io.confluent.ksql.parser.tree.FunctionCall;
import io.confluent.ksql.parser.tree.GroupBy;
import io.confluent.ksql.parser.tree.GroupingElement;
import io.confluent.ksql.parser.tree.InListExpression;
import io.confluent.ksql.parser.tree.InPredicate;
import io.confluent.ksql.parser.tree.InsertInto;
import io.confluent.ksql.parser.tree.IsNotNullPredicate;
import io.confluent.ksql.parser.tree.IsNullPredicate;
import io.confluent.ksql.parser.tree.Join;
import io.confluent.ksql.parser.tree.JoinOn;
import io.confluent.ksql.parser.tree.LikePredicate;
import io.confluent.ksql.parser.tree.LogicalBinaryExpression;
import io.confluent.ksql.parser.tree.NotExpression;
import io.confluent.ksql.parser.tree.NullIfExpression;
import io.confluent.ksql.parser.tree.Query;
import io.confluent.ksql.parser.tree.QuerySpecification;
import io.confluent.ksql.parser.tree.Relation;
import io.confluent.ksql.parser.tree.SampledRelation;
import io.confluent.ksql.parser.tree.SearchedCaseExpression;
import io.confluent.ksql.parser.tree.Select;
import io.confluent.ksql.parser.tree.SelectItem;
import io.confluent.ksql.parser.tree.SetOperation;
import io.confluent.ksql.parser.tree.SimpleCaseExpression;
import io.confluent.ksql.parser.tree.SimpleGroupBy;
import io.confluent.ksql.parser.tree.SingleColumn;
import io.confluent.ksql.parser.tree.Statement;
import io.confluent.ksql.parser.tree.Statements;
import io.confluent.ksql.parser.tree.Struct;
import io.confluent.ksql.parser.tree.SubqueryExpression;
import io.confluent.ksql.parser.tree.SubscriptExpression;
import io.confluent.ksql.parser.tree.TableSubquery;
import io.confluent.ksql.parser.tree.Type;
import io.confluent.ksql.parser.tree.Values;
import io.confluent.ksql.parser.tree.WhenClause;
import io.confluent.ksql.parser.tree.Window;
import io.confluent.ksql.parser.tree.WindowFrame;
import io.confluent.ksql.parser.tree.WithQuery;
import io.confluent.ksql.util.Pair;

import java.util.Set;

public abstract class DefaultTraversalVisitor<R, C>
    extends AstVisitor<R, C> {

  @Override
  protected R visitStatements(Statements node, C context) {
    for (Statement statement : node.statementList) {
      process(statement, context);
    }
    return visitNode(node, context);
  }

  @Override
  protected R visitExtract(Extract node, C context) {
    return process(node.getExpression(), context);
  }

  @Override
  protected R visitCast(Cast node, C context) {
    return process(node.getExpression(), context);
  }

  @Override
  protected R visitArithmeticBinary(ArithmeticBinaryExpression node, C context) {
    process(node.getLeft(), context);
    process(node.getRight(), context);

    return null;
  }

  @Override
  protected R visitBetweenPredicate(BetweenPredicate node, C context) {
    process(node.getValue(), context);
    process(node.getMin(), context);
    process(node.getMax(), context);

    return null;
  }

  @Override
  protected R visitSubscriptExpression(SubscriptExpression node, C context) {
    process(node.getBase(), context);
    process(node.getIndex(), context);

    return null;
  }

  @Override
  protected R visitComparisonExpression(ComparisonExpression node, C context) {
    process(node.getLeft(), context);
    process(node.getRight(), context);

    return null;
  }

  @Override
  protected R visitQuery(Query node, C context) {

    process(node.getQueryBody(), context);
    return null;
  }

  @Override
  protected R visitWithQuery(WithQuery node, C context) {
    return process(node.getQuery(), context);
  }

  @Override
  protected R visitSelect(Select node, C context) {
    for (SelectItem item : node.getSelectItems()) {
      process(item, context);
    }

    return null;
  }

  @Override
  protected R visitSingleColumn(SingleColumn node, C context) {
    process(node.getExpression(), context);

    return null;
  }

  @Override
  protected R visitWhenClause(WhenClause node, C context) {
    process(node.getOperand(), context);
    process(node.getResult(), context);

    return null;
  }

  @Override
  protected R visitInPredicate(InPredicate node, C context) {
    process(node.getValue(), context);
    process(node.getValueList(), context);

    return null;
  }

  @Override
  protected R visitFunctionCall(FunctionCall node, C context) {
    for (Expression argument : node.getArguments()) {
      process(argument, context);
    }

    if (node.getWindow().isPresent()) {
      process(node.getWindow().get(), context);
    }

    return null;
  }

  @Override
  protected R visitDereferenceExpression(DereferenceExpression node, C context) {
    process(node.getBase(), context);
    return null;
  }

  @Override
  public R visitWindow(Window node, C context) {
    process(node.getWindowExpression(), context);
    return null;
  }

  @Override
  public R visitWindowFrame(WindowFrame node, C context) {
    process(node.getStart(), context);
    if (node.getEnd().isPresent()) {
      process(node.getEnd().get(), context);
    }

    return null;
  }

  @Override
  public R visitFrameBound(FrameBound node, C context) {
    if (node.getValue().isPresent()) {
      process(node.getValue().get(), context);
    }

    return null;
  }

  @Override
  protected R visitSimpleCaseExpression(SimpleCaseExpression node, C context) {
    process(node.getOperand(), context);
    for (WhenClause clause : node.getWhenClauses()) {
      process(clause, context);
    }

    node.getDefaultValue()
        .ifPresent(value -> process(value, context));

    return null;
  }

  @Override
  protected R visitInListExpression(InListExpression node, C context) {
    for (Expression value : node.getValues()) {
      process(value, context);
    }

    return null;
  }

  @Override
  protected R visitNullIfExpression(NullIfExpression node, C context) {
    process(node.getFirst(), context);
    process(node.getSecond(), context);

    return null;
  }

  @Override
  protected R visitArithmeticUnary(ArithmeticUnaryExpression node, C context) {
    return process(node.getValue(), context);
  }

  @Override
  protected R visitNotExpression(NotExpression node, C context) {
    return process(node.getValue(), context);
  }

  @Override
  protected R visitSearchedCaseExpression(SearchedCaseExpression node, C context) {
    for (WhenClause clause : node.getWhenClauses()) {
      process(clause, context);
    }
    node.getDefaultValue()
        .ifPresent(value -> process(value, context));

    return null;
  }

  @Override
  protected R visitLikePredicate(LikePredicate node, C context) {
    process(node.getValue(), context);
    process(node.getPattern(), context);
    if (node.getEscape() != null) {
      process(node.getEscape(), context);
    }

    return null;
  }

  @Override
  protected R visitIsNotNullPredicate(IsNotNullPredicate node, C context) {
    return process(node.getValue(), context);
  }

  @Override
  protected R visitIsNullPredicate(IsNullPredicate node, C context) {
    return process(node.getValue(), context);
  }

  @Override
  protected R visitLogicalBinaryExpression(LogicalBinaryExpression node, C context) {
    process(node.getLeft(), context);
    process(node.getRight(), context);

    return null;
  }

  @Override
  protected R visitSubqueryExpression(SubqueryExpression node, C context) {
    return process(node.getQuery(), context);
  }


  @Override
  protected R visitQuerySpecification(QuerySpecification node, C context) {
    process(node.getSelect(), context);
    process(node.getFrom(), context);

    if (node.getWhere().isPresent()) {
      process(node.getWhere().get(), context);
    }
    if (node.getGroupBy().isPresent()) {
      process(node.getGroupBy().get(), context);
    }
    if (node.getHaving().isPresent()) {
      process(node.getHaving().get(), context);
    }
    return null;
  }

  @Override
  protected R visitSetOperation(SetOperation node, C context) {
    for (Relation relation : node.getRelations()) {
      process(relation, context);
    }
    return null;
  }

  @Override
  protected R visitValues(Values node, C context) {
    for (Expression row : node.getRows()) {
      process(row, context);
    }
    return null;
  }

  @Override
  protected R visitStruct(Struct node, C context) {
    for (Pair<String, Type> structItem : node.getItems()) {
      process(structItem.getRight(), context);
    }
    return null;
  }

  @Override
  protected R visitTableSubquery(TableSubquery node, C context) {
    return process(node.getQuery(), context);
  }

  @Override
  protected R visitAliasedRelation(AliasedRelation node, C context) {
    return process(node.getRelation(), context);
  }

  @Override
  protected R visitSampledRelation(SampledRelation node, C context) {
    process(node.getRelation(), context);
    process(node.getSamplePercentage(), context);
    if (node.getColumnsToStratifyOn().isPresent()) {
      for (Expression expression : node.getColumnsToStratifyOn().get()) {
        process(expression, context);
      }
    }
    return null;
  }

  @Override
  protected R visitJoin(Join node, C context) {
    process(node.getLeft(), context);
    process(node.getRight(), context);

    node.getCriteria()
        .filter(criteria -> criteria instanceof JoinOn)
        .map(criteria -> process(((JoinOn) criteria).getExpression(), context));

    return null;
  }

  @Override
  protected R visitGroupBy(GroupBy node, C context) {
    for (GroupingElement groupingElement : node.getGroupingElements()) {
      process(groupingElement, context);
    }

    return null;
  }

  @Override
  protected R visitGroupingElement(GroupingElement node, C context) {
    for (Set<Expression> expressions : node.enumerateGroupingSets()) {
      for (Expression expression : expressions) {
        process(expression, context);
      }
    }
    return null;
  }

  @Override
  protected R visitSimpleGroupBy(SimpleGroupBy node, C context) {
    visitGroupingElement(node, context);

    for (Expression expression : node.getColumnExpressions()) {
      process(expression, context);
    }

    return null;
  }

  @Override
  protected R visitDelete(Delete node, C context) {
    process(node.getTable(), context);
    node.getWhere().ifPresent(where -> process(where, context));

    return null;
  }

  @Override
  protected R visitCreateTableAsSelect(CreateTableAsSelect node, C context) {
    process(node.getQuery(), context);
    node.getProperties().values().forEach(expression -> process(expression, context));

    return null;
  }

  @Override
  protected R visitInsertInto(InsertInto node, C context) {
    process(node.getQuery(), context);
    return null;
  }
}
