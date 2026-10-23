/* NovaBank 화면 공통 스크립트 (jQuery 1.12) */
$(function () {
    // 금액 입력: 포커스 잃으면 천 단위 구분 기호
    $(document).on('blur', 'input.money', function () {
        var digits = $(this).val().replace(/[^0-9]/g, '');
        if (digits) {
            $(this).val(Number(digits).toLocaleString());
        }
    });

    // 이체 화면: 은행·금액이 바뀌면 수수료 미리보기
    var $form = $('#transferForm');
    if ($form.length) {
        var url = $form.data('fee-url');
        var timer = null;
        var preview = function () {
            var amount = $('#amount').val().replace(/[^0-9]/g, '');
            if (!amount) {
                $('#feePreview').text('');
                return;
            }
            $.getJSON(url, { toBank: $('#toBank').val(), amount: amount })
                .done(function (r) {
                    $('#feePreview').text('예상 수수료 ' + Number(r.fee).toLocaleString() + '원, 출금 합계 '
                        + Number(r.totalDebit).toLocaleString() + '원' + (r.businessHours ? '' : ' (영업시간 외 가산 포함)'));
                })
                .fail(function () {
                    $('#feePreview').text('수수료를 계산할 수 없습니다. 입력값을 확인하세요.');
                });
        };
        $form.on('change keyup', '#toBank, #amount', function () {
            clearTimeout(timer);
            timer = setTimeout(preview, 300);
        });
        // 당행이면 계좌번호 예시를 채워 준다
        $('#toBank').on('change', function () {
            if ($(this).val() === 'NOVA' && !$('#toAccountNo').val()) {
                $('#toAccountNo').val('110-0917-000002');
            }
        });
    }

    // 알림 메시지는 5초 뒤 사라진다
    var $alert = $('.alert-info');
    if ($alert.length) {
        setTimeout(function () { $alert.fadeOut(400); }, 5000);
    }
});
