package com.shop.ecommerce.exceptionHandler;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError<E> {
    private Integer status;
    private Exception<E> exception;
}
