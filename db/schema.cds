namespace flights.db;
using { Currency } from '@sap/cds/common';

@cds.autoexpose
entity Departments {
    key name      : String(20);
        budget    : Decimal(9,2);
        currency  : Currency;
        employees : Composition of many Employees
                    on employees.department = $self; 
}

@cds.autoexpose
entity Employees {
    key eid        : String(8);
        name       : String(20);
        department : Association to one Departments;
        flights    : Composition of many Flights
                     on flights.employee = $self; 
}

@cds.autoexpose
entity Flights {
    key id          : Integer64;
        employee    : Association to Employees;
        origin      : Association to one Airports;
        destination : Association to one Airports;
        airline     : String(20);
        price       : Decimal(9,2);
        currency    : Currency;
}

@cds.autoexpose
entity Airports {
  key iata       : String(3);
      city       : Association to Cities;
      weather    : Association to WeatherReports;
      departures : Association to many Flights
                   on departures.origin = $self;
      arrivals   : Association to many Flights
                   on arrivals.destination = $self;
}

@cds.autoexpose
entity Cities {
  key name    : String(20);
      country : String(20);
      airports: Composition of many Airports
                on airports.city = $self;
}

type WeatherCondition : {
  description : String;
  temperature : Decimal(5, 2);
}

@cds.autoexpose
entity WeatherReports {
  key id      : Integer64;
      current : WeatherCondition;
      airport : Association to Airports
                on airport.weather = $self;
}