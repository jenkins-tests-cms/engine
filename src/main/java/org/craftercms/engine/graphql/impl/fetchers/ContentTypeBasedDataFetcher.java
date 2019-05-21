/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.engine.graphql.impl.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.Selection;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.craftercms.search.elasticsearch.ElasticsearchWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StopWatch;

import static org.craftercms.engine.graphql.SchemaUtils.*;

/**
 * Implementation of {@link DataFetcher} that queries Elasticsearch to retrieve content based on a content type.
 * @author joseross
 * @since 3.1
 */
public class ContentTypeBasedDataFetcher implements DataFetcher<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeBasedDataFetcher.class);

    private static final String QUERY_FIELD_NAME_CONTENT_TYPE = getOriginalName(FIELD_NAME_CONTENT_TYPE);

    // Lucene regexes always match the entire string, no need to specify ^ or $
    public static final String CONTENT_TYPE_REGEX_PAGE = "/?page/.*";
    public static final String CONTENT_TYPE_REGEX_COMPONENT = "/?component/.*";

    /**
     * The default value for the 'limit' argument
     */
    protected int defaultLimit;

    /**
     * The default value for the 'sortBy' argument
     */
    protected String defaultSortField;

    /**
     * The default value for the 'sortOrder' argument
     */
    protected String defaultSortOrder;

    /**
     * The instance of {@link ElasticsearchWrapper}
     */
    protected ElasticsearchWrapper elasticsearch;

    @Required
    public void setDefaultLimit(final int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    @Required
    public void setDefaultSortField(final String defaultSortField) {
        this.defaultSortField = defaultSortField;
    }

    @Required
    public void setDefaultSortOrder(final String defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    @Required
    public void setElasticsearch(final ElasticsearchWrapper elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final DataFetchingEnvironment env) {
        Field field = env.getMergedField().getSingleField();
        String fieldName = field.getName();

        // Get arguments for pagination & sorting
        int offset = Optional.ofNullable(env.<Integer>getArgument(ARG_NAME_OFFSET)).orElse(0);
        int limit = Optional.ofNullable(env.<Integer>getArgument(ARG_NAME_LIMIT)).orElse(defaultLimit);
        String sortBy = Optional.ofNullable(env.<String>getArgument(ARG_NAME_SORT_BY)).orElse(defaultSortField);
        String sortOrder = Optional.ofNullable(env.<String>getArgument(ARG_NAME_SORT_ORDER)).orElse(defaultSortOrder);

        List<String> queryFieldIncludes = new LinkedList<>();
        // Add content-type to includes, we might need it for a GraphQL TypeResolver
        queryFieldIncludes.add(QUERY_FIELD_NAME_CONTENT_TYPE);

        List<Map<String, Object>> items = new LinkedList<>();
        Map<String, Object> result = new HashMap<>(2);
        result.put(FIELD_NAME_ITEMS, items);

        // Setup the ES query
        SearchSourceBuilder source = new SearchSourceBuilder();
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        source
            .query(query)
            .from(offset)
            .size(limit)
            .sort(sortBy, SortOrder.fromString(sortOrder));

        StopWatch watch = new StopWatch(field.getName() + " - " + field.getAlias());

        watch.start("build filters");

        // Filter by the content-type
        if (fieldName.equals(FIELD_NAME_CONTENT_ITEMS)) {
            query.filter(QueryBuilders.existsQuery(QUERY_FIELD_NAME_CONTENT_TYPE));
        } else if (fieldName.equals(FIELD_NAME_PAGES)) {
            query.filter(QueryBuilders.regexpQuery(QUERY_FIELD_NAME_CONTENT_TYPE, CONTENT_TYPE_REGEX_PAGE));
        } else if (fieldName.equals(FIELD_NAME_COMPONENTS)) {
            query.filter(QueryBuilders.regexpQuery(QUERY_FIELD_NAME_CONTENT_TYPE, CONTENT_TYPE_REGEX_COMPONENT));
        } else {
            // Get the content-type name from the field name
            query.filter(QueryBuilders.termQuery(QUERY_FIELD_NAME_CONTENT_TYPE, getOriginalName(fieldName)));
        }

        // Check the selected fields to build the ES query
        Optional<Field> itemsField = field.getSelectionSet().getSelections()
                            .stream()
                            .map(f -> (Field) f)
                            .filter(f -> f.getName().equals(FIELD_NAME_ITEMS))
                            .findFirst();
        if (itemsField.isPresent()) {
            List<Selection> selections = itemsField.get().getSelectionSet().getSelections();
            selections.forEach(selection -> processSelection(StringUtils.EMPTY, selection, query, queryFieldIncludes,
             env));
        }

        // Only fetch the selected fields for better performance
        source.fetchSource(queryFieldIncludes.toArray(new String[0]), new String[0]);
        watch.stop();

        logger.debug("Executing query: {}", source);

        watch.start("searching items");
        SearchResponse response = elasticsearch.search(new SearchRequest().source(source));
        watch.stop();

        watch.start("processing items");
        result.put(FIELD_NAME_TOTAL, response.getHits().totalHits);
        if (response.getHits().totalHits > 0) {
            for(SearchHit hit :  response.getHits().getHits()) {
                items.add(fixItems(hit.getSourceAsMap()));
            }
        }
        watch.stop();

        if (logger.isTraceEnabled()) {
            logger.trace(watch.prettyPrint());
        }

        return result;
    }

    /**
     * Adds the required filters to the ES query for the given field
     */
    protected void processSelection(String path, Selection currentSelection, BoolQueryBuilder query,
                                    List<String> queryFieldIncludes, DataFetchingEnvironment env)  {
        if (currentSelection instanceof Field) {
            // If the current selection is a field
            Field currentField = (Field) currentSelection;

            // Get the original field name
            String propertyName = getOriginalName(currentField.getName());
            // Build the ES-friendly path
            String fullPath = StringUtils.isEmpty(path)? propertyName : path + "." + propertyName;

            // If the field has sub selection
            if (Objects.nonNull(currentField.getSelectionSet())) {
                // Process recursively and finish
                currentField.getSelectionSet().getSelections()
                    .forEach(selection -> processSelection(fullPath, selection, query, queryFieldIncludes, env));
                return;
            }

            // Add the field to the list
            logger.debug("Adding selected field '{}' to query", fullPath);
            queryFieldIncludes.add(fullPath);

            // Check the filters to build the ES query
            Optional<Argument> arg =
                currentField.getArguments().stream().filter(a -> a.getName().equals(FILTER_NAME)).findFirst();
            if (arg.isPresent() && arg.get().getValue() instanceof ObjectValue) {
                logger.debug("Adding filters for field {}", fullPath);

                List<ObjectField> filters = ((ObjectValue) arg.get().getValue()).getObjectFields();
                filters.forEach((filter) -> {
                    switch (filter.getName()) {
                        case ARG_NAME_EQUALS:
                            query.filter(QueryBuilders.termQuery(fullPath, getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_MATCHES:
                            query.filter(QueryBuilders.matchQuery(fullPath, getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_REGEX:
                            query.filter(
                                QueryBuilders.regexpQuery(fullPath, getRealValue(filter.getValue()).toString()));
                            break;
                        case ARG_NAME_LT:
                            query.filter(QueryBuilders.rangeQuery(fullPath).lt(getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_GT:
                            query.filter(QueryBuilders.rangeQuery(fullPath).gt(getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_LTE:
                            query.filter(QueryBuilders.rangeQuery(fullPath).lte(getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_GTE:
                            query.filter(QueryBuilders.rangeQuery(fullPath).gte(getRealValue(filter.getValue())));
                            break;
                        case ARG_NAME_EXISTS:
                            boolean exists = (boolean) getRealValue(filter.getValue());
                            if (exists) {
                                query.filter(QueryBuilders.existsQuery(fullPath));
                            } else {
                                query.mustNot(QueryBuilders.existsQuery(fullPath));
                            }
                            break;
                        default:
                            // never happens
                    }
                });
            }
        } else if (currentSelection instanceof InlineFragment) {
            // If the current selection is an inline fragment, process recursively
            InlineFragment fragment = (InlineFragment) currentSelection;
            fragment.getSelectionSet().getSelections()
                .forEach(selection -> processSelection(path, selection, query, queryFieldIncludes, env));
        } else if (currentSelection instanceof FragmentSpread) {
            // If the current selection is a fragment spread, find the fragment and process recursively
            FragmentSpread fragmentSpread = (FragmentSpread) currentSelection;
            FragmentDefinition fragmentDefinition = env.getFragmentsByName().get(fragmentSpread.getName());
            fragmentDefinition.getSelectionSet().getSelections()
                .forEach(selection -> processSelection(path, selection, query, queryFieldIncludes, env));
        }
    }

    /**
     * Extracts a scalar value, this is needed because of GraphQL strict types
     */
    protected Object getRealValue(Value value) {
        if (value instanceof BooleanValue) {
            return ((BooleanValue)value).isValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        return null;
    }

    /**
     * Checks for fields containing the 'item' keyword and makes sure they are always a list even if there is only
     * one value. This is needed because the GraphQL schema always needs to return the same type for a field.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> fixItems(Map<String, Object> map) {
        Map<String, Object> temp = new LinkedHashMap<>();

        map.forEach((key, value) -> {
            String graphQLKey = getGraphQLName(key);
            if (FIELD_NAME_ITEM.equals(key)) {
                if (!(value instanceof List)) {
                    temp.put(graphQLKey, Collections.singletonList(fixItems((Map<String, Object>) value)));
                }
            } else if (value instanceof Map) {
                temp.put(graphQLKey, fixItems((Map<String, Object>) value));
            } else {
                temp.put(graphQLKey, value);
            }
        });

        return MapUtils.isNotEmpty(temp)? temp : map;
    }

}
