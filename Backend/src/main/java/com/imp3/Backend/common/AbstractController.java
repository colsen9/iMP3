package com.imp3.Backend.common;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class AbstractController {

    protected Integer getSessionUid(HttpSession session){
        Integer uid = (Integer)session.getAttribute("uid");
        if(uid == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }

}
