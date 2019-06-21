/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.base.persistence.facade;

import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author sajabrayilov
 * @param <T>
 */

public interface LazyModel<T> {
    public List<T> findAllPaginated (int first, int pageSize);
}
