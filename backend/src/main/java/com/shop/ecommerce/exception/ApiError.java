package com.shop.ecommerce.exception;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError<T> {
    private String id;
    private Date errorTime;
    private T errors;
}
