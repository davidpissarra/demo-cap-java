using { flights.db as db } from '../db/schema';

@path:'/browse'
service FlightsService {
  entity Departments as SELECT from db.Departments;
  entity Employees as SELECT from db.Employees;
  entity Flights as projection on db.Flights;
  @readonly entity Cities as SELECT from db.Cities;
  @readonly entity Airports as projection on db.Airports;
  @readonly entity Weather as SELECT from db.WeatherReports;
}
