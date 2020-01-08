public class Main {
    public void main(StateEnum state) {
        if (state == StateEnum.A) {
            System.out.println("A");
        } else if (state == StateEnum.B) {
            System.out.println("B");
        } else if (state == StateEnum.C) {
            System.out.println("C");
        }
    }

    private enum StateEnum {
        A, B, C
    }
}