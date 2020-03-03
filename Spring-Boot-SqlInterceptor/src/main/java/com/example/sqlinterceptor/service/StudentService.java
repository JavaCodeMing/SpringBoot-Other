package com.example.sqlinterceptor.service;

import com.example.sqlinterceptor.bean.Student;

/**
 * Created by dengzhiming on 2019/3/21
 */
public interface StudentService {
    Student queryStudentById(String sno);
}
