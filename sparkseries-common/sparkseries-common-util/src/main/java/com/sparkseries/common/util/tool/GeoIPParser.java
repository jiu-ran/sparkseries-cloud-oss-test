package com.sparkseries.common.util.tool;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * IP 解析器
 */
@Slf4j
public class GeoIPParser {
    private final DatabaseReader reader;

    public GeoIPParser(String dbPath) throws Exception {
        File database = new File(dbPath);
        reader = new DatabaseReader.Builder(database)
                .locales(Arrays.asList("zh-CN", "en"))
                .build();
    }

    /**
     * 根据 IP 获取位置信息
     */
    public String getLocation(String ip) {
        try {
            if (InternalIpChecker.isInternalIp(ip)) {
                return "内网地址";
            }

            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            // 提取关键信息
            Country country = response.getCountry();
            Subdivision subdivision = response.getMostSpecificSubdivision();
            City city = response.getCity();
            Location location = response.getLocation();

            return String.format(
                    "国家: %s (%s), 省份/州: %s, 城市: %s, 经纬度: [%f, %f], 时区: %s",
                    country.getName(),
                    country.getIsoCode(),
                    subdivision.getName(),
                    city.getName(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getTimeZone()
            );
        } catch (AddressNotFoundException e) {
            return "未知";
        } catch (Exception e) {
            log.error("IP 解析失败,原因: {}", e.getMessage());
            return "解析失败";
        }
    }
}
