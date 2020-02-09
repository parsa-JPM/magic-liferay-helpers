package dpco.library.parsa.classes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Junction;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.service.BaseLocalService;

/**
 * 
 * 
 * make easier life to crate dynamic query
 *
 * @author p.mihandoost
 * @version 1.0.3
 */
public class SimpleQuery<T> {

	/*
	 * model of query
	 */
	private Class<T> modelClass;

	/**
	 * use to make query and run it
	 */
	private DynamicQuery dyQuery;

	/**
	 * class object of service utill to run and get dynamic query
	 */
	private Class<?> xxxLocalserviceUtill;

	/**
	 * to OR field in where clause
	 */
	private Disjunction disjunction;

	/**
	 * to ADD field in where clause
	 */
	private Junction junction;
	/**
	 * it can use when we want to use injectedService
	 */
	private BaseLocalService localService;

	private static final String BIN_OPT_AND = "and";
	private static final String BIN_OPT_OR = "or";

	/**
	 * it use to decide to choose binary operator for where clause
	 */
	private String binOperator = "and";

	/**
	 * it's default constructor. give model class object and create dynamic query
	 * from it
	 *
	 * @param modelClass
	 */
	public SimpleQuery(Class<T> modelClass) {
		this.modelClass = modelClass;
		xxxLocalserviceUtill = getServiceUtillClassObj(modelClass);
		initDynamicQuery();
		junction = RestrictionsFactoryUtil.conjunction();
		disjunction = RestrictionsFactoryUtil.disjunction();
	}

	/**
	 * give model class object and create dynamic query from it and give injected
	 * local service to run query
	 *
	 * @param modelClass
	 */
	public SimpleQuery(Class<T> modelClass, BaseLocalService service) {
		localService = service;
		this.modelClass = modelClass;
		xxxLocalserviceUtill = service.getClass();
		initDynamicQuery();
		junction = RestrictionsFactoryUtil.conjunction();
		disjunction = RestrictionsFactoryUtil.disjunction();
	}

	/**
	 * create new model with auto increment
	 *
	 * @return
	 */
	public T newModel() {
		long id = CounterLocalServiceUtil.increment(modelClass.getName());
		try {
			Method createNewModel = xxxLocalserviceUtill.getMethod("create" + modelClass.getSimpleName(), long.class);
			return (T) invokeMethod(createNewModel, id);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * save new model object (new record) to the DB
	 *
	 * @param model
	 * @return T
	 */
	public T save(T model) {
		try {
			Method addModel = xxxLocalserviceUtill.getMethod("add" + modelClass.getSimpleName(), modelClass);
			return (T) invokeMethod(addModel, model);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * update existing model
	 *
	 * @param model
	 *
	 * @return T
	 */
	public T update(T model) {
		try {
			Method updateModel = xxxLocalserviceUtill.getMethod("update" + modelClass.getSimpleName(), modelClass);
			return (T) invokeMethod(updateModel, model);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * change binary operator to and (if you call this then call where methods they
	 * will be AND until call or method to reverse)
	 *
	 * @return this
	 */
	public SimpleQuery<T> and() {
		binOperator = BIN_OPT_AND;
		return this;
	}

	/**
	 * change binary operator to or (if you call this then call where methods they
	 * will be OR until call and method to reverse)
	 *
	 * @return this
	 */
	public SimpleQuery<T> or() {
		binOperator = BIN_OPT_OR;
		return this;
	}

	/**
	 * default where that it assume your operate is equal
	 *
	 * @param column
	 * @param value
	 * @return this
	 */
	public SimpleQuery<T> where(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.eq(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.eq(column, value));

		return this;
	}

	/**
	 * create where with dynamic operator ('=','>','<', ...)
	 *
	 * @param column
	 * @param operator
	 * @param value
	 * @return SimpleQuery<T>
	 * @throws Exception
	 */
	public SimpleQuery<T> where(String column, String operator, Object value) {

		operator = operator.trim();

		if (operator.equals(">")) {
			gt(column, value);
			return this;
		}

		if (operator.equals(">=")) {
			ge(column, value);
			return this;
		}

		if (operator.equals("<")) {
			lt(column, value);
			return this;
		}

		if (operator.equals("<=")) {
			le(column, value);
			return this;
		}

		if (operator.equals("<>") || operator.equals("!=")) {
			notEqual(column, value);
			return this;
		}

		return null;

	}

	/**
	 * conver string to date then call where method
	 *
	 *
	 * @param column
	 * @param operator
	 * @param value
	 *
	 * @return SimpleQuery<T>
	 */
	public SimpleQuery<T> whereDate(String column, String operator, Object value) {

		if (value instanceof Date)
			return where(column, operator, value);

		if (value instanceof String) {
			Date date = getDate((String) value);
			return where(column, operator, date);
		}

		// I guess it throws exception if your column type is date
		return where(column, operator, value);
	}

	/**
	 * add like query
	 *
	 * @param column
	 * @param value
	 * @return
	 */
	public SimpleQuery<T> whereLike(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.like(column, "%" + value + "%"));
		else
			disjunction.add(RestrictionsFactoryUtil.like(column, "%" + value + "%"));

		return this;
	}

	/**
	 * add like query (reverse)
	 *
	 * @param column
	 * @param value
	 * @return
	 */
	public SimpleQuery<T> whereNotLike(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.not(RestrictionsFactoryUtil.like(column, "%" + value + "%")));
		else
			disjunction.add(RestrictionsFactoryUtil.not(RestrictionsFactoryUtil.like(column, "%" + value + "%")));

		return this;
	}

	/**
	 * check column that is a null or not
	 *
	 * @param column
	 * @return this
	 */
	public SimpleQuery<T> whereNull(String column) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.isNull(column));
		else
			disjunction.add(RestrictionsFactoryUtil.isNull(column));

		return this;
	}

	/**
	 * check column that is a null or not (reverse)
	 *
	 * @param column
	 * @return this
	 */
	public SimpleQuery<T> whereNotNull(String column) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.isNotNull(column));
		else
			disjunction.add(RestrictionsFactoryUtil.isNotNull(column));

		return this;
	}

	/**
	 * make in SQL clause
	 * 
	 * 
	 * @param propertyName
	 * @param values
	 * @return this
	 */
	public SimpleQuery<T> whereIn(String propertyName, Collection<?> values) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.in(propertyName, values));
		else
			disjunction.add(RestrictionsFactoryUtil.in(propertyName, values));

		return this;
	}

	/**
	 * 
	 * it does join with select in select
	 * 
	 * @param pivoteTable    it's model class object of second table
	 * @param searchColName  defines which col will be searched
	 * @param searchColvalue value of search col (above col)
	 * @param projectionCol  defines which col will be selected in inner select to
	 *                       use in main IN query (in outer select)
	 * @param inColName      define a col from main (current) model that use in main
	 *                       IN query (in outer select)
	 */
	public SimpleQuery<T> whereIn(Class<?> pivoteTable, String searchColName, Collection<?> searchColvalue,
			String projectionCol, String inColName) {
		// inner select IN
		DynamicQuery pivoteQuery = DynamicQueryFactoryUtil.forClass(pivoteTable, "pivote",
				PortletClassLoaderUtil.getClassLoader());
		pivoteQuery.add(RestrictionsFactoryUtil.in("pivote." + searchColName, searchColvalue));
		pivoteQuery.setProjection(PropertyFactoryUtil.forName("pivote." + projectionCol));

		// outer select IN
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(PropertyFactoryUtil.forName(inColName).in(pivoteQuery));
		else
			disjunction.add(PropertyFactoryUtil.forName(inColName).in(pivoteQuery));

		return this;
	}

	/**
	 * 
	 * it does join with select in select
	 * 
	 * @param innerQuery     defines inner select query (it must specify projection column)
	 * @param inColName      define a col from main (current) model that use in main
	 *                       IN query (in outer select)
	 */
	public SimpleQuery<T> whereIn(SimpleQuery<?> innerQuery, String inColName) {
		// outer select IN
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(PropertyFactoryUtil.forName(inColName).in(innerQuery.makeReadyDynamicQuery().dyQuery));
		else
			disjunction.add(PropertyFactoryUtil.forName(inColName).in(innerQuery.makeReadyDynamicQuery().dyQuery));

		return this;
	}

	/**
	 * order data by a column
	 *
	 * @param column
	 * @param orderType
	 * @return
	 */
	public SimpleQuery<T> orderBy(String column, String orderType) {

		Order order = null;

		if (orderType.equalsIgnoreCase("asc"))
			order = OrderFactoryUtil.asc(column);

		if (orderType.equalsIgnoreCase("desc"))
			order = OrderFactoryUtil.desc(column);

		dyQuery.addOrder(order);

		return this;
	}

	/**
	 * set column that return by query result
	 * 
	 * @param colName
	 * @return this
	 */
	public SimpleQuery<T> setProjection(String colName) {
		dyQuery.setProjection(PropertyFactoryUtil.forName(colName));
		return this;
	}

	/**
	 * group by a column NOTE : when you use this method dynamic query can't return
	 * list of models , it will just return list of string that those are group by
	 * column values
	 * 
	 * @param col
	 * @return SimpleQuery<T>
	 */
	public SimpleQuery<T> groupBy(String col) {

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();
		projectionList.add(ProjectionFactoryUtil.groupProperty(col));

		dyQuery.setProjection(projectionList);

		return this;
	}

	/**
	 * set limit on query
	 *
	 * @param start
	 * @param end
	 * @return
	 */
	public SimpleQuery<T> setLimit(int start, int end) {

		dyQuery.setLimit(start, end);

		return this;
	}

	/**
	 * get generated dynamic query
	 * 
	 * @return DynamicQuery
	 */
	public DynamicQuery getDynamicQuery() {
		return dyQuery;
	}

	/**
	 * make ready to use dynamic query 
	 */
	public SimpleQuery<T> makeReadyDynamicQuery() {
		junction.add(disjunction);
		dyQuery.add(junction);
		
		return this;
	}
	
	
	/**
	 * get result of query
	 *
	 * @return List<T>
	 */
	public List<T> get() {
		junction.add(disjunction);
		dyQuery.add(junction);
		return runQuery();
	}

	/**
	 * get result of query with dynamic return type
	 *
	 * @return List<R>
	 */
	public <R> List<R> get(Class<R> returnType) {
		junction.add(disjunction);
		dyQuery.add(junction);
		return runQuery(returnType);
	}

	/**
	 * get first model of result list
	 *
	 * @return T
	 */
	public T first() {
		try {
			if (get().size() == 0)
				throw new ModelNotFoundException("Not found any " + modelClass.getSimpleName() + " object");

			return get().get(0);
		} catch (ModelNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 *
	 * it wrap model with optional to avoid nullPointerException
	 *
	 * @return
	 */
	public Optional<T> safeFirst() {
		if (get().size() == 0)
			return Optional.ofNullable(null);
		return Optional.ofNullable(get().get(0));
	}

	/**
	 * add grater than condition
	 *
	 * @param column
	 * @param value
	 */
	private void gt(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.gt(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.gt(column, value));
	}

	/**
	 * add lower than condition
	 *
	 * @param column
	 * @param value
	 */
	private void lt(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.lt(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.lt(column, value));
	}

	/**
	 * add lower or equal condition
	 *
	 * @param column
	 * @param value
	 */
	private void le(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.le(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.le(column, value));
	}

	/**
	 * add grater or equal condition
	 *
	 * @param column
	 * @param value
	 */
	private void ge(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.ge(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.ge(column, value));
	}

	/**
	 * add not equal operator
	 */
	private void notEqual(String column, Object value) {
		// now we can just do AND & OR clause
		if (binOperator.equalsIgnoreCase(BIN_OPT_AND))
			junction.add(RestrictionsFactoryUtil.ne(column, value));
		else
			disjunction.add(RestrictionsFactoryUtil.ne(column, value));
	}

	/**
	 * run generated query with dynamic
	 *
	 * @return R
	 */
	private <R> List<R> runQuery(Class<R> returnType) {
		try {

			Method method = xxxLocalserviceUtill.getMethod("dynamicQuery", DynamicQuery.class);

			return (List<R>) invokeMethod(method, dyQuery);

		} catch (NoSuchMethodException | SecurityException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}

		return null;
	}

	/**
	 * run generated query
	 *
	 * @return
	 */
	private List<T> runQuery() {
		try {

			Method method = xxxLocalserviceUtill.getMethod("dynamicQuery", DynamicQuery.class);

			return (List<T>) invokeMethod(method, dyQuery);

		} catch (NoSuchMethodException | SecurityException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}

		return null;
	}

	/**
	 * init dynamic query with service utill class object
	 */
	private void initDynamicQuery() {
		try {

			Method method = xxxLocalserviceUtill.getMethod("dynamicQuery", null);

			dyQuery = (DynamicQuery) invokeMethod(method, null);

		} catch (NoSuchMethodException | SecurityException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}
	}

	/**
	 * it work same as `method.invoke` but it check localService is null or not and
	 * do best way
	 * 
	 * @param method
	 * @param args
	 * @return Object
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private Object invokeMethod(Method method, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		if (localService != null) {
			return method.invoke(localService, args);
		} else {
			return method.invoke(null, args);
		}

	}

	/**
	 * get XXXLocalServiceUtill class object
	 *
	 * @param model
	 * @return Class<?>
	 */
	private Class<?> getServiceUtillClassObj(Class<?> model) {
		// make package of XXXLocalServiceUtill to find it
		String modelName = model.getSimpleName();
		String qualifiedNameOfService = model.getName().replace("model", "service").replace(modelName,
				modelName + "LocalServiceUtil");

		try {
			return Class.forName(qualifiedNameOfService);
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return null;
		}
	}

	/**
	 * convert string to expected date
	 *
	 * @param date
	 * @return
	 */
	private Date getDate(String date) {
		if (date == null || date.isEmpty())
			return null;

		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}

class ModelNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7911948838004813911L;

	public ModelNotFoundException(String string) {
		super(string);
	}

}
