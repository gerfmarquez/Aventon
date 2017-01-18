package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "aventon-mobilehub-1870610676-trips")

public class TripsDO {
    private String _userId;
    private String _dropoffAddress;
    private Double _dropoffCoordinates;
    private String _pickupAddress;
    private Double _pickupCoordinates;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBAttribute(attributeName = "userId")
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBAttribute(attributeName = "dropoff_address")
    public String getDropoffAddress() {
        return _dropoffAddress;
    }

    public void setDropoffAddress(final String _dropoffAddress) {
        this._dropoffAddress = _dropoffAddress;
    }
    @DynamoDBAttribute(attributeName = "dropoff_coordinates")
    public Double getDropoffCoordinates() {
        return _dropoffCoordinates;
    }

    public void setDropoffCoordinates(final Double _dropoffCoordinates) {
        this._dropoffCoordinates = _dropoffCoordinates;
    }
    @DynamoDBAttribute(attributeName = "pickup_address")
    public String getPickupAddress() {
        return _pickupAddress;
    }

    public void setPickupAddress(final String _pickupAddress) {
        this._pickupAddress = _pickupAddress;
    }
    @DynamoDBAttribute(attributeName = "pickup_coordinates")
    public Double getPickupCoordinates() {
        return _pickupCoordinates;
    }

    public void setPickupCoordinates(final Double _pickupCoordinates) {
        this._pickupCoordinates = _pickupCoordinates;
    }

}
