package com.ssafy.naeda.domain.store.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicStoreCsvLoader {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final String BAKERY_CATEGORY_ID = "PUBLIC_BAKERY";
    private static final String RESTAURANT_CATEGORY_ID = "PUBLIC_RESTAURANT";

    private final ResourceLoader resourceLoader;

    @Value("#{'${store.seed.resources:classpath:seed/stores/gumi_bakery.csv,classpath:seed/stores/gumi_restaurant.csv}'.split(',')}")
    private List<String> resourceLocations;

    public List<PublicStoreCsvRecord> loadActiveStores() {
        List<PublicStoreCsvRecord> stores = new ArrayList<>();
        for (String location : resourceLocations) {
            stores.addAll(loadFromLocation(location.trim()));
        }
        return stores;
    }

    public String calculateContentHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String location : resourceLocations) {
                Resource resource = resourceLoader.getResource(location.trim());
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("CSV 시드 파일을 읽을 수 없습니다: " + location, e);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시를 생성할 수 없습니다.", e);
        }
    }

    private List<PublicStoreCsvRecord> loadFromLocation(String location) {
        Resource resource = resourceLoader.getResource(location);
        String defaultCategoryId = location.contains("bakery") ? BAKERY_CATEGORY_ID : RESTAURANT_CATEGORY_ID;
        List<PublicStoreCsvRecord> stores = new ArrayList<>();

        try (
                InputStream inputStream = resource.getInputStream();
                Reader reader = new InputStreamReader(inputStream, CSV_CHARSET);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                if (!isActive(record)) {
                    continue;
                }

                String sourceKey = trimToNull(get(record, "관리번호"));
                String storeName = trimToNull(get(record, "사업장명"));
                if (sourceKey == null || storeName == null) {
                    continue;
                }

                String rawCategoryName = trimToNull(get(record, "위생업태명"));
                if (rawCategoryName == null) {
                    rawCategoryName = trimToNull(get(record, "업태구분명"));
                }

                stores.add(new PublicStoreCsvRecord(
                        sourceKey,
                        storeName,
                        defaultCategoryId,
                        rawCategoryName == null ? defaultCategoryId : rawCategoryName,
                        firstNonBlank(
                                trimToNull(get(record, "도로명주소")),
                                trimToNull(get(record, "지번주소"))
                        ),
                        trimToNull(get(record, "지번주소")),
                        trimToNull(get(record, "전화번호")),
                        parseDouble(get(record, "좌표정보(X)")),
                        parseDouble(get(record, "좌표정보(Y)"))
                ));
            }
            return stores;
        } catch (IOException e) {
            throw new IllegalStateException("CSV 시드 파일 파싱에 실패했습니다: " + location, e);
        }
    }

    private boolean isActive(CSVRecord record) {
        String businessStatus = trimToNull(get(record, "영업상태명"));
        String detailStatus = trimToNull(get(record, "상세영업상태명"));
        String statusCode = trimToNull(get(record, "영업상태코드"));
        return "영업/정상".equals(businessStatus)
                || ("영업".equals(detailStatus) && "01".equals(statusCode));
    }

    private String get(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header) : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private Double parseDouble(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
