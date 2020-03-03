package com.example.sqlinterceptor.service.impl;

import com.example.sqlinterceptor.bean.Student;
import com.example.sqlinterceptor.mapper.StudentMapper;
import com.example.sqlinterceptor.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by dengzhiming on 2019/3/21
 */
@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentMapper studentMapper;

    @Override
    public Student queryStudentById(String sno) {
        return this.studentMapper.queryStudentById(sno);
    }
}
