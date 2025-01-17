package io.rtdi.bigdata.connector.properties.atomic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.rtdi.bigdata.connector.pipeline.foundation.exceptions.PropertiesException;

@JsonTypeInfo(
		  use = JsonTypeInfo.Id.NAME, 
		  include = JsonTypeInfo.As.PROPERTY, 
		  property = "type")
@JsonSubTypes({ 
	@Type(value = PropertyString.class, name = "PropertyString"), 
	@Type(value = PropertyText.class, name = "PropertyText"), 
	@Type(value = PropertyArrayList.class, name = "PropertyArrayList"),
	@Type(value = PropertyInt.class, name = "PropertyInt"),
	@Type(value = PropertyLong.class, name = "PropertyLong"), 
	@Type(value = PropertyGroup.class, name = "PropertyGroup"), 
	@Type(value = PropertyPassword.class, name = "PropertyPassword"), 
	@Type(value = PropertyBoolean.class, name = "PropertyBoolean") 
})
public interface IProperty {

	String getName();

	IProperty clone(boolean ignorepasswords) throws PropertiesException;
	
}
