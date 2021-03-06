package org.hibernate.query.validator;

import org.hibernate.*;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.*;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.*;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.sql.SelectFragment;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ClassType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

import javax.persistence.AccessType;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.hibernate.query.validator.MockSessionFactory.typeHelper;

abstract class MockEntityPersister implements EntityPersister, Queryable, DiscriminatorMetadata {

    private static final String[] ID_COLUMN = {"id"};

    private final String entityName;
    private final MockSessionFactory factory;
    private final List<MockEntityPersister> subclassPersisters = new ArrayList<>();
    final AccessType defaultAccessType;
    private final Map<String,Type> propertyTypesByName = new HashMap<>();

    MockEntityPersister(String entityName,
                        AccessType defaultAccessType,
                        MockSessionFactory factory) {
        this.entityName = entityName;
        this.factory = factory;
        this.defaultAccessType = defaultAccessType;
    }

    void initSubclassPersisters() {
        for (MockEntityPersister other: factory.getMockEntityPersisters()) {
            other.addPersister(this);
            this.addPersister(other);
        }
    }

    private void addPersister(MockEntityPersister entityPersister) {
        if (isSubclassPersister(entityPersister)) {
            subclassPersisters.add(entityPersister);
        }
    }

    private Type getSubclassPropertyType(String propertyPath) {
        return subclassPersisters.stream()
                .map(sp -> sp.getPropertyType(propertyPath))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    abstract boolean isSubclassPersister(MockEntityPersister entityPersister);

    @Override
    public SessionFactoryImplementor getFactory() {
        return factory;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public final Type getPropertyType(String propertyPath) throws MappingException {
        Type result = propertyTypesByName.get(propertyPath);
        if (result!=null) {
            return result;
        }

        result = createPropertyType(propertyPath);
        if (result == null) {
            //check subclasses, needed for treat()
            result = getSubclassPropertyType(propertyPath);
        }

        if (result!=null) {
            propertyTypesByName.put(propertyPath, result);
        }
        return result;
    }

    abstract Type createPropertyType(String propertyPath);

    @Override
    public Type getIdentifierType() {
        //TODO: propertyType(getIdentifierPropertyName())
        return StandardBasicTypes.INTEGER;
    }

    @Override
    public String getIdentifierPropertyName() {
        //TODO!!!!!!
        return "id";
    }

    @Override
    public Type toType(String propertyName) throws QueryException {
        Type type = getPropertyType(propertyName);
        if (type == null) {
            throw new QueryException(getEntityName()
                    + " has no mapped "
                    + propertyName);
        }
        return type;
    }

    @Override
    public String getRootEntityName() {
        return entityName;
    }

    @Override
    public Declarer getSubclassPropertyDeclarer(String s) {
        return Declarer.CLASS;
    }

    @Override
    public String[] toColumns(String alias, String propertyName)
            throws QueryException {
        return new String[] { "" };
    }

    @Override
    public String[] toColumns(String propertyName)
            throws QueryException, UnsupportedOperationException {
        return new String[] { "" };
    }

    @Override
    public Type getType() {
        return typeHelper.entity(entityName);
    }

    @Override
    public Serializable[] getPropertySpaces() {
        return new Serializable[] {entityName};
    }

    @Override
    public Serializable[] getQuerySpaces() {
        return new Serializable[] {entityName};
    }

    @Override
    public void generateEntityDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityMetamodel getEntityMetamodel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void postInstantiate() throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableRole getNavigableRole() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityEntryFactory getEntityEntryFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSubclassEntityName(String s) {
        return false;
    }

    @Override
    public boolean hasProxy() {
        return false;
    }

    @Override
    public boolean hasCollections() {
        return false;
    }

    @Override
    public boolean hasMutableProperties() {
        return false;
    }

    @Override
    public boolean hasSubselectLoadableCollections() {
        return false;
    }

    @Override
    public boolean hasCascades() {
        return false;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean isInherited() {
        return false;
    }

    @Override
    public boolean isIdentifierAssignedByInsert() {
        return false;
    }

    @Override
    public int[] findDirty(Object[] objects, Object[] objects1, Object o,
                           SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] findModified(Object[] objects, Object[] objects1, Object o,
                              SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasIdentifierProperty() {
        return false;
    }

    @Override
    public boolean canExtractIdOutOfEntity() {
        return false;
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public VersionType getVersionType() {
        return null;
    }

    @Override
    public int getVersionProperty() {
        return -66;
    }

    @Override
    public boolean hasNaturalIdentifier() {
        return false;
    }

    @Override
    public int[] getNaturalIdentifierProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getNaturalIdentifierSnapshot(Serializable serializable,
                                                 SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierGenerator getIdentifierGenerator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasLazyProperties() {
        return false;
    }

    @Override
    public Serializable loadEntityIdByNaturalId(Object[] objects, LockOptions lockOptions,
                                                SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Serializable serializable, Object o, LockMode lockMode,
                       SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Serializable serializable, Object o, LockOptions lockOptions,
                       SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List multiLoad(Serializable[] serializables,
                          SharedSessionContractImplementor sharedSessionContractImplementor,
                          MultiLoadOptions multiLoadOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock(Serializable serializable, Object o, Object o1, LockMode lockMode,
                     SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock(Serializable serializable, Object o, Object o1, LockOptions lockOptions,
                     SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(Serializable serializable, Object[] objects, Object o,
                       SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable insert(Object[] objects, Object o,
                               SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Serializable serializable, Object o, Object o1,
                       SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Serializable serializable, Object[] objects, int[] ints, boolean b,
                       Object[] objects1, Object o, Object o1, Object o2,
                       SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type[] getPropertyTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyInsertability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueInclusion[] getPropertyInsertGenerationInclusions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyUpdateability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyCheckability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyNullability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyVersionability() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean[] getPropertyLaziness() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CascadeStyle[] getPropertyCascadeStyles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCacheInvalidationRequired() {
        return false;
    }

    @Override
    public boolean isLazyPropertiesCacheable() {
        return false;
    }

    @Override
    public boolean canReadFromCache() {
        return false;
    }

    @Override
    public boolean canWriteToCache() {
        return false;
    }

    @Override
    public boolean hasCache() {
        return false;
    }

    @Override
    public EntityDataAccess getCacheAccessStrategy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheEntryStructure getCacheEntryStructure() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheEntry buildCacheEntry(Object o, Object[] objects, Object o1,
                                      SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNaturalIdCache() {
        return false;
    }

    @Override
    public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassMetadata getClassMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBatchLoadable() {
        return false;
    }

    @Override
    public boolean isSelectBeforeUpdateRequired() {
        return false;
    }

    @Override
    public Object[] getDatabaseSnapshot(Serializable serializable,
                                        SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdByUniqueKey(Serializable serializable, String s,
                                         SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getCurrentVersion(Serializable serializable,
                                    SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object forceVersionIncrement(Serializable serializable, Object o,
                                        SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInstrumented() {
        return false;
    }

    @Override
    public boolean hasInsertGeneratedProperties() {
        return false;
    }

    @Override
    public boolean hasUpdateGeneratedProperties() {
        return false;
    }

    @Override
    public boolean isVersionPropertyGenerated() {
        return false;
    }

    @Override
    public void afterInitialize(Object o,
                                SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterReassociate(Object o,
                                 SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object createProxy(Serializable serializable,
                              SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isTransient(Object o,
                               SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getPropertyValuesToInsert(Object o, Map map,
                                              SharedSessionContractImplementor sharedSessionContractImplementor)
            throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processInsertGeneratedProperties(Serializable serializable, Object o, Object[] objects,
                                                 SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processUpdateGeneratedProperties(Serializable serializable, Object o, Object[] objects,
                                                 SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class getMappedClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean implementsLifecycle() {
        return false;
    }

    @Override
    public Class getConcreteProxyClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValues(Object o, Object[] objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValue(Object o, int i, Object o1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getPropertyValues(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getPropertyValue(Object o, int i) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getPropertyValue(Object o, String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdentifier(Object o) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdentifier(Object o,
                                      SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIdentifier(Object o, Serializable serializable,
                              SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVersion(Object o) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object instantiate(Serializable serializable,
                              SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInstance(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasUninitializedLazyProperties(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetIdentifier(Object o, Serializable serializable, Object o1,
                                SharedSessionContractImplementor sharedSessionContractImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityPersister getSubclassEntityPersister(Object o, SessionFactoryImplementor sessionFactoryImplementor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityMode getEntityMode() {
        return EntityMode.POJO;
    }

    @Override
    public EntityTuplizer getEntityTuplizer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytecodeEnhancementMetadata getInstrumentationMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterAliasGenerator getFilterAliasGenerator(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] resolveAttributeIndexes(String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUseReferenceCacheEntries() {
        return false;
    }

    @Override
    public EntityPersister getEntityPersister() {
        return this;
    }

    @Override
    public EntityIdentifierDefinition getEntityKeyDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<AttributeDefinition> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public void registerAffectingFetchProfile(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTableAliasForColumn(String s, String s1) {
        return "";
    }

    @Override
    public boolean isExplicitPolymorphism() {
        return false;
    }

    @Override
    public String getMappedSuperclass() {
        return null;
    }

    @Override
    public String getDiscriminatorSQLValue() {
        return "";
    }

    @Override
    public String identifierSelectFragment(String name, String suffix) {
        return "";
    }

    @Override
    public String propertySelectFragment(String alias, String suffix, boolean b) {
        return "";
    }

    @Override
    public SelectFragment propertySelectFragmentFragment(String alias, String suffix, boolean b) {
        return new SelectFragment();
    }

    @Override
    public boolean hasSubclasses() {
        return false;
    }

    @Override
    public Type getDiscriminatorType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getDiscriminatorValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSubclassForDiscriminatorValue(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getKeyColumnNames() {
        return getIdentifierColumnNames();
    }

    @Override
    public String[] getIdentifierColumnNames() {
        return ID_COLUMN;
    }

    @Override
    public String[] getIdentifierAliases(String suffix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPropertyAliases(String suffix, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPropertyColumnNames(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDiscriminatorAlias(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDiscriminatorColumnName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasRowId() {
        return false;
    }

    @Override
    public Object[] hydrate(ResultSet resultSet, Serializable serializable, Object o, Loadable loadable, String[][] strings, boolean b, SharedSessionContractImplementor sharedSessionContractImplementor) throws SQLException, HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultiTable() {
        return false;
    }

    @Override
    public String[] getConstraintOrderedTableNameClosure() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[][] getContraintOrderedTableKeyColumnClosure() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSubclassPropertyTableNumber(String propertyPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSubclassTableName(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersionPropertyInsertable() {
        return false;
    }

    @Override
    public String generateFilterConditionAlias(String s) {
        return "";
    }

    @Override
    public DiscriminatorMetadata getTypeDiscriminatorMetadata() {
        return this;
    }

    @Override
    public String getSqlFragment(String sqlQualificationAlias) {
        return "";
    }

    @Override
    public Type getResolutionType() {
        return ClassType.INSTANCE;
    }

    @Override
    public String[][] getSubclassPropertyFormulaTemplateClosure() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTableName() {
        return entityName;
    }

    @Override
    public String selectFragment(Joinable joinable, String s, String s1, String s2, String s3, boolean b) {
        return "";
    }

    @Override
    public String whereJoinFragment(String s, boolean b, boolean b1) {
        return "";
    }

    @Override
    public String whereJoinFragment(String s, boolean b, boolean b1, Set<String> set) {
        return "";
    }

    @Override
    public String fromJoinFragment(String s, boolean b, boolean b1) {
        return "";
    }

    @Override
    public String fromJoinFragment(String s, boolean b, boolean b1, Set<String> set) {
        return "";
    }

    @Override
    public String filterFragment(String s, Map map) throws MappingException {
        return "";
    }

    @Override
    public String filterFragment(String s, Map map, Set<String> set) throws MappingException {
        return "";
    }

    @Override
    public String oneToManyFilterFragment(String s) throws MappingException {
        return "";
    }

    @Override
    public String oneToManyFilterFragment(String s, Set<String> set) {
        return "";
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public boolean consumesEntityAlias() {
        return true;
    }

    @Override
    public boolean consumesCollectionAlias() {
        return false;
    }

    @Override
    public String toString() {
        return "MockEntityPersister[" + entityName + "]";
    }

}
