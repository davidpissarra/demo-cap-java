package customer.demo_cap_java.handlers;

import java.lang.Long;
import java.util.stream.Stream;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;


import cds.gen.flightsservice.Flights;
import cds.gen.flightsservice.Flights_;
import cds.gen.flightsservice.Employees;
import cds.gen.flightsservice.Employees_;
import cds.gen.flightsservice.Departments;
import cds.gen.flightsservice.Departments_;
import cds.gen.flightsservice.FlightsService_;

import customer.demo_cap_java.MessageKeys;

@Component
@ServiceName(FlightsService_.CDS_NAME)
public class FlightsServiceHandler implements EventHandler {

	@Autowired
	PersistenceService db;

	@Before(event = CdsService.EVENT_CREATE)
	public void reduceDepartmentBudget(Stream<Flights> flights) {  
		flights.forEach(flight -> {

            // Get employee's department name
            Result result = db.run(
                Select
                  .from(Employees_.class)
                  .columns(emp -> emp.department_name())
                  .where(emp -> emp.eid().eq(flight.getEmployeeEid()))
            );
            Employees employee = result.first(Employees.class).orElseThrow(notFound(MessageKeys.EMPLOYEE_MISSING));
            
            // Get department budget
            result = db.run(
                Select
                  .from(Departments_.class)
                  .columns(dep -> dep.budget())
                  .where(dep -> dep.name().eq(employee.getDepartmentName()))
            );
            Departments department = result.first(Departments.class).orElseThrow(notFound(MessageKeys.DEPARTMENT_MISSING));

            // Subtract the price of the flight from the department budget
            department.setBudget(department.getBudget().subtract(flight.getPrice()));

            // Verify if budget is sufficient (not negative)
            if(department.getBudget().intValue() < 0) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST, MessageKeys.INSUFFICIENT_BUDGET);
            }

            db.run(
                Update
                  .entity(Departments_.class)
                  .data(department)
                  .byId(employee.getDepartmentName())
            );
        });
    }

    @Before(event = CdsService.EVENT_DELETE)
	public void increaseDepartmentBudget(CdsDeleteEventContext context) {
        CqnAnalyzer cqnAnalyzer = CqnAnalyzer.create(context.getModel());
        AnalysisResult analysisResult = cqnAnalyzer.analyze(context.getCqn());
        Long flightID = (Long) analysisResult.targetKeys().get("id");

        Result result = db.run(
                Select
                  .from(Flights_.class)
                  .columns(f -> f.price(), f -> f.employee_eid())
                  .byId(flightID)
            );
        Flights flight = result.first(Flights.class).orElseThrow(notFound(MessageKeys.FLIGHT_MISSING));

        result = db.run(
                Select
                  .from(Employees_.class)
                  .columns(emp -> emp.department_name())
                  .byId(flight.getEmployeeEid())
            );
        Employees employee = result.first(Employees.class).orElseThrow(notFound(MessageKeys.EMPLOYEE_MISSING));

        result = db.run(
                Select
                  .from(Departments_.class)
                  .columns(dep -> dep.budget())
                  .where(dep -> dep.name().eq(employee.getDepartmentName()))
            );
        Departments department = result.first(Departments.class).orElseThrow(notFound(MessageKeys.DEPARTMENT_MISSING));

        department.setBudget(department.getBudget().add(flight.getPrice()));

        db.run(
            Update
              .entity(Departments_.class)
              .data(department)
              .byId(employee.getDepartmentName())
        );
    }
    
    private Supplier<ServiceException> notFound(String message) {
		return () -> new ServiceException(ErrorStatuses.NOT_FOUND, message);
	}

}