package com.cibertec.skyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cibertec.skyflow.entity.NotaTarea;


@Repository
public interface NotaTareaRepository extends JpaRepository<NotaTarea, Long> {

	List<NotaTarea> findByTareaIdOrderByFechaCreacionDesc(Long tareaId);
}
