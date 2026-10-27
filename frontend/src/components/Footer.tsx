export default function Footer() {
    return (
        <footer className="footer">
            <div className="container">
                <div className="footer-grid">
                    <div>
                        <h4>مونیکو شاپ</h4>
                        <p style={{fontSize: 13, lineHeight: 1.8}}>
                            فروشگاه تخصصی لوازم آرایشی، بهداشتی و مراقبتی با تضمین اصالت کالا
                        </p>
                    </div>
                    <div>
                        <h4>دسترسی سریع</h4>
                        <ul>
                            <li><a href="/">صفحه اصلی</a></li>
                            <li><a href="/about">درباره ما</a></li>
                            <li><a href="/contact">تماس با ما</a></li>
                        </ul>
                    </div>
                    <div>
                        <h4>خدمات مشتریان</h4>
                        <ul>
                            <li><a href="#">پیگیری سفارش</a></li>
                            <li><a href="#">سوالات متداول</a></li>
                            <li><a href="#">راهنمای خرید</a></li>
                        </ul>
                    </div>
                    <div>
                        <h4>تماس با ما</h4>
                        <ul>
                            <li>
                                تلفن: <a href="tel:+989028178300" dir="ltr" style={{unicodeBidi: 'isolate'}}>+98 902 817
                                8300</a>
                            </li>
                            <li>
                                تلفن: <a href="tel:+989028178400" dir="ltr" style={{unicodeBidi: 'isolate'}}>+98 902 817
                                8400</a>
                            </li>
                            <li>ایمیل: info@mail.moniko.ir</li>
                        </ul>
                        <div className="enamad-seal" style={{marginTop: 12}}>
                            <a
                                referrerPolicy="origin"
                                target="_blank"
                                href="https://trustseal.enamad.ir/?id=8365443&Code=ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS"
                            >
                                <img
                                    referrerPolicy="origin"
                                    src="https://trustseal.enamad.ir/logo.aspx?id=8365443&Code=ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS"
                                    style={{cursor: 'pointer'}}
                                    {...({code: 'ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS'} as Record<string, string>)}
                                />
                            </a>
                        </div>
                    </div>
                </div>
                <div className="footer-bottom">
                    © ۱۴۰۴ مونیکو شاپ - تمامی حقوق محفوظ است
                </div>
            </div>
        </footer>
    );
}