package com.ecole.sales.security;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserArg {}
