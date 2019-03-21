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

package org.craftercms.engine.graphql.impl.field;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.craftercms.core.util.XmlUtils;
import org.craftercms.engine.graphql.GraphQLFieldFactory;
import org.dom4j.Document;
import org.dom4j.Node;
import org.springframework.beans.factory.annotation.Required;

import static graphql.Scalars.GraphQLString;
import static org.craftercms.engine.graphql.SchemaUtils.FIELD_SUFFIX_RAW;
import static org.craftercms.engine.graphql.SchemaUtils.TEXT_FILTER;
import static org.craftercms.engine.graphql.SchemaUtils.getGraphQLName;

/**
 * Implementation of {@link GraphQLFieldFactory} that handles RTE fields. It adds the copy field with the original
 * HTML markup without any filter to prevent queries to it
 * @author joseross
 * @since 3.1
 */
public class RTEFieldFactory implements GraphQLFieldFactory {

    protected String contentTypePropertyTitleXpath;

    @Required
    public void setContentTypePropertyTitleXpath(final String contentTypePropertyTitleXpath) {
        this.contentTypePropertyTitleXpath = contentTypePropertyTitleXpath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createField(final Document definition, final Node property, final String fieldId,
                            final String typeName, final String fieldName, final GraphQLObjectType.Builder newType,
                            final GraphQLFieldDefinition.Builder newField) {
        // Add the copy field as string without filters
        newType.field(GraphQLFieldDefinition.newFieldDefinition()
            .name(getGraphQLName(fieldName) + FIELD_SUFFIX_RAW)
            .description(XmlUtils.selectSingleNodeValue(property, contentTypePropertyTitleXpath))
            .type(GraphQLString));

        // Add the original as string with text filters
        newField.type(GraphQLString);
        newField.argument(TEXT_FILTER);
    }

}
