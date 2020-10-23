import a;

function init() {
   a:incrementCount();
   a:assertCount(2);
}

public function sample() {

}

listener a:ABC ep = new a:ABC("ModB");
