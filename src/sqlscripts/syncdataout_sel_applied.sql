/* список по возрастанию ID последних операций при создании чека (добавление оплаты) 
   до даты параметра ? включительно */
select sdo.ID /*, sdo.TABLENAME, sdo.TABLEKEY1IDNAME, sdo.TABLEKEY1IDVAL*/
    from APP.SYNCDATAOUT sdo
    inner join APP.PAYMENTS p on p.ID=sdo.TABLEKEY1IDVAL
    where sdo.OTYPE='I' and sdo.Status>0 and sdo.TableName='APP.PAYMENTS' and DATE(sdo.UPDATEDATE)<=? order by sdo.ID
