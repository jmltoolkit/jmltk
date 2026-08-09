public class Locals {
    public void foo() {
        int local = 0;
        //@ ghost int jmlLocal = local;
        //@ assert jmlLocal == 0;       // Totally valid
        local = jmlLocal;               // This is an error!
    }
}

//? e java2jml: jmlLocal@(line 6,col 17)
//? name: jmlLocal@(line 5,col 20) to jmlLocal@(line 4,col 19)
//? name: local@(line 4,col 34) to local@(line 3,col 9)
//? type: jmlLocal@(line 5,col 20)
//? type: jmlLocal@(line 6,col 17)
//? type: local@(line 4,col 34)
//? name: local@(line 6,col 9) to local@(line 3,col 9)
//? type: local@(line 6,col 9)
