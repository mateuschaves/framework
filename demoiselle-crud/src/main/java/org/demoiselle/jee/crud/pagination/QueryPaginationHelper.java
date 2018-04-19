package org.demoiselle.jee.crud.pagination;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import org.demoiselle.jee.core.api.crud.Result;
import org.demoiselle.jee.crud.configuration.DemoiselleCrudConfig;
import org.demoiselle.jee.crud.count.QueryCountHelper;
import org.demoiselle.jee.crud.field.QueryFieldsHelper;
import org.demoiselle.jee.crud.fields.FieldsContext;
import org.demoiselle.jee.crud.filter.FilterContext;

/**
 * A Helper class that paginates results based on the PaginationContext and the FilterContext.
 * Both of them are given and are supposed to either be manually created or built from the request parameters.
 *
 * @param <T> The Entity Class of the query
 */
public class QueryPaginationHelper<T> {

    /**
     * The pagination context with the current pagination parameters.
     */
    private final PaginationContext paginationContext;
    private QueryCountHelper<T> queryCountHelper;

    /**
     * The global configuration for the Demoiselle CRUD class. Obtained as a RequestScoped bean.
     */
    private DemoiselleCrudConfig crudConfig;

    /**
     * The EntityManager for the query.
     */
    private EntityManager entityManager;

    /**
     * The root entity class for the query.
     */
    private Class<T> entityClass;

    /**
     * The filter context with the current query filter parameters.
     */
    private FieldsContext fieldsContext;
    private FilterContext filterContext;

    /**
     * Create a new instance of the Helper for an EntityManager, an entity class and pagination/filter parameters
     * @param em The entity manager that will be used for the query.
     * @param crudConfig The current CRUD configuration, usually for the request
     * @param entityClass The root entity class of the root query.
     * @param paginationContext The current pagination context/parameters.
     * @param fieldsContext The current fields context/parameters
     * @param filterContext The current filter context/parameters
     * @param <T> The entity class type
     * @return A new QueryPaginationHelper instance for the givewn parameters.
     */
    public static <T> QueryPaginationHelper<T> createFor(EntityManager em, DemoiselleCrudConfig crudConfig, Class<T> entityClass, PaginationContext paginationContext, FieldsContext fieldsContext, FilterContext filterContext) {
        return new QueryPaginationHelper<>(em, crudConfig, entityClass, paginationContext, fieldsContext, filterContext, new QueryCountHelper<>(em, entityClass));
    }

    public QueryPaginationHelper(EntityManager entityManager, DemoiselleCrudConfig crudConfig, Class<T> entityClass, PaginationContext paginationContext, FieldsContext fieldsContext, FilterContext filterContext, QueryCountHelper<T> queryCountHelper) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.fieldsContext = fieldsContext;
        this.filterContext = filterContext;
        this.crudConfig = crudConfig;
        this.paginationContext = paginationContext;
        this.queryCountHelper = queryCountHelper;
    }


    /**
     * Get the paginated result for the query, considering the current parameters of this specific QueryPaginationHelper instance.
     *
     * @param criteriaQuery The JPA criteria query that will be paginated
     * @return The result list wrapped in a {@link Result} object, which will contain parameters such as the limit/offset for the pagination and the
     * search parameters.
     */
    public ResultSet getPaginatedResult(CriteriaQuery<T> criteriaQuery) {
        ResultSet result = new ResultSet();
        result.setFieldsContext(fieldsContext);
        TypedQuery<T> query = QueryFieldsHelper.createFilteredQuery(entityManager, criteriaQuery, entityClass, fieldsContext);
        if (paginationContext.isPaginationEnabled()) {
            Integer firstResult = paginationContext.getOffset() == null ? 0 : paginationContext.getOffset();
            Integer maxResults = getMaxResult();
            Long count = getResultCount(criteriaQuery);
            if (firstResult < count) {
                query.setFirstResult(firstResult);
                query.setMaxResults(maxResults);
            }

            result.setCount(count);
        }
        result.setContent(query.getResultList());
        if (result.getContent() != null && !result.getContent().isEmpty()
                && paginationContext.isPaginationEnabled()
                && result.getContent().size() <= result.getCount()
                && result.getCount() < getMaxResult()) {
            paginationContext.setLimit(result.getCount().intValue());
        }
        result.setResultType(entityClass);
        result.setPaginationContext(paginationContext);

        return result;
    }

    private Integer getMaxResult() {
        if (paginationContext.getLimit() == null || paginationContext.getOffset() == null) {
            return crudConfig.getDefaultPagination();
        }

        return (paginationContext.getLimit() - paginationContext.getOffset()) + 1;
    }

    private Long getResultCount(CriteriaQuery criteriaQuery) {
        return queryCountHelper.getResultCount(criteriaQuery, filterContext);
    }
}
