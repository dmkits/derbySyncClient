/*данные первой не обработанной (примененной на сервере) записи из таблицы синхронизации */
select ID, TABLENAME, OTYPE, TABLEKEY1IDNAME, TABLEKEY1IDVAL, TABLEKEY2IDNAME, TABLEKEY2IDVAL 
    from APP.SYNCDATAOUT 
    where ID= (select min(ID) from APP.SYNCDATAOUT where STATUS<=0)
    order by ID
