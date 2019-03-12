package org.mltds.sargeras.test.example;

import org.mltds.sargeras.api.SagaContext;
import org.mltds.sargeras.api.SagaTx;
import org.mltds.sargeras.api.SagaTxStatus;

public class BookHotel implements SagaTx {
    public static final String HOTEL_ORDER_NO = "HOTEL_ORDER_NO";

    private boolean bookSucc;

    public BookHotel() {
        this(true);
    }

    public BookHotel(boolean bookSucc) {
        this.bookSucc = bookSucc;
    }

    @Override
    public SagaTxStatus execute(SagaContext context) {

        if (bookSucc) {
            // 预约酒店成功，对方返回一个酒店预约单号
            String bookHotelOrderNo = "hotel456";
            context.savePersistentInfo(HOTEL_ORDER_NO, bookHotelOrderNo);
            return SagaTxStatus.SUCCESS;
        } else {
            context.setBizResult("预定酒店失败，别去了~~~");
            return SagaTxStatus.EXE_FAIL_TO_COMP;
        }

    }

    @Override
    public SagaTxStatus compensate(SagaContext context) {
        String bookHotelOrderNo = context.getPersistentInfo(HOTEL_ORDER_NO, String.class);
        // 用这个 bookHotelOrderNo 取消预定
        return SagaTxStatus.SUCCESS;
    }
}