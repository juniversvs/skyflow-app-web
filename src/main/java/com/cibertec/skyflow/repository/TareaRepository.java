package com.cibertec.skyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cibertec.skyflow.entity.EstadoTarea;
import com.cibertec.skyflow.entity.Tarea;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

	List<Tarea> findByProyectoId(Long proyectoId);

	long countByProyectoId(Long proyectoId);

	long countByProyectoIdAndEstado(Long proyectoId, EstadoTarea estado);
}
