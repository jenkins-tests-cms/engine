/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.engine.cache;

import org.craftercms.engine.service.context.SiteContext;

/**
 * Helper class that's used to warm up the cache of a site context.
 *
 * @author avasquez
 * @since 3.1.4
 */
public interface SiteCacheWarmer {

    /**
     * Warms up the cache for the specified site context. If {@code switchCache} is true, then a new cache is created,
     * warmed up and then switched with the current cache of the site.
     *
     * @param siteContext the site context
     * @param switchCache if a new cache should be warmed and then switched with the current cache
     */
    void warmUpCache(SiteContext siteContext, boolean switchCache);

}
