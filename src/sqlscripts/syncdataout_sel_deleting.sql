/* Исходящие данные синхронизации для удаления в порядке убывания ID начиная с ID заданного параметром ? */
select * from APP.SYNCDATAOUT where Status>0 and ID<=? order by ID desc