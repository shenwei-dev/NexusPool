package com.family.chat.model;

public class LocationMessage {

    /** 主地名 */
    private String title;

    /** 距离信息 */
    private String distance;

    /** 街道路牌 */
    private String roadAddress;

    /** 经度 */
    private double longitude;

    /** 纬度 */
    private double latitude;

    /** 全参构造方法 */
    public LocationMessage(String title, String distance, String roadAddress, double latitude, double longitude) {
        this.title = title;
        this.distance = distance;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Getter */
    public String getTitle() { return title; }
    public String getDistance() { return distance; }
    public String getRoadAddress() { return roadAddress; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }


}