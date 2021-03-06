package com.expediagroup.pact.podam;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Map;

import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.typeManufacturers.AbstractTypeManufacturer;
import uk.co.jemos.podam.typeManufacturers.IntTypeManufacturerImpl;

public class BigIntegerManufacturer extends AbstractTypeManufacturer<BigInteger> {

    @Override
    public BigInteger getType(DataProviderStrategy strategy,
                              AttributeMetadata attributeMetadata,
                              Map<String, Type> genericTypesArgumentsMap) {
        return BigInteger.valueOf(
                new IntTypeManufacturerImpl().getType(strategy, attributeMetadata, genericTypesArgumentsMap));
    }
}
